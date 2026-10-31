#!/usr/bin/env python3
"""Verify the executable server JAR across fresh JVMs in a disposable MySQL catalog.

Requires Java 21, a built server JAR, and the normal local db.properties. The
original JAR/configuration are never edited. Only this script's catalog is cleared.
"""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASE_URL = 'http://127.0.0.1:8080'


def request(method, path, expected=200, body=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token is not None:
        headers['authorization'] = token
    data = None if body is None else json.dumps(body).encode()
    req = Request(BASE_URL + path, data=data, headers=headers, method=method)
    try:
        response = urlopen(req, timeout=3)
    except HTTPError as error:
        response = error
    with response:
        status = response.status
        result = json.load(response)
    if status != expected:
        raise AssertionError(f'{method} {path}: expected {expected}, received {status}')
    return result


def isolated_jar(source, config, destination, catalog):
    properties, count = re.subn(rb'(?m)^db\.name\s*=.*$',
                                b'db.name=' + catalog.encode(), config.read_bytes())
    if count != 1:
        raise ValueError('Configuration must contain exactly one db.name property')
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(destination, 'w') as isolated:
        if original.namelist().count('db.properties') != 1:
            raise ValueError('Packaged server must contain exactly one db.properties resource')
        for entry in original.infolist():
            content = properties if entry.filename == 'db.properties' else original.read(entry)
            isolated.writestr(entry, content)
    destination.chmod(0o600)


class ServerProcess:
    def __init__(self, java, jar, directory):
        self.java = java
        self.jar = jar
        self.directory = directory
        self.process = None
        self.starts = 0
        self.log = None

    def start(self):
        # Do not accidentally run requests against a user's existing server.
        with socket.socket() as probe:
            probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            probe.bind(('127.0.0.1', 8080))
        self.starts += 1
        self.log = (self.directory / f'server-{self.starts}.log').open('wb')
        self.process = subprocess.Popen([self.java, '-jar', str(self.jar)],
                                        stdout=self.log, stderr=subprocess.STDOUT)
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            if self.process.poll() is not None:
                raise RuntimeError('Packaged server exited before becoming ready')
            try:
                request('GET', '/game', expected=401)
                return
            except (OSError, URLError):
                time.sleep(0.1)
        raise TimeoutError('Packaged server did not start within 20 seconds')

    def stop(self):
        if self.process is not None:
            if self.process.poll() is None:
                self.process.terminate()
                try:
                    self.process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    self.process.kill()
                    self.process.wait(timeout=5)
            self.process = None
        if self.log is not None:
            self.log.close()
            self.log = None

    def restart(self):
        self.stop()
        self.start()


def verify_restarts(server):
    server.start()
    white = request('POST', '/user', body={
        'username': 'white', 'password': 'white-test-password', 'email': 'white@example.test'})['authToken']
    black = request('POST', '/user', body={
        'username': 'black', 'password': 'black-test-password', 'email': 'black@example.test'})['authToken']
    white_login = request('POST', '/session', body={
        'username': 'white', 'password': 'white-test-password'})['authToken']
    game_id = request('POST', '/game', body={'gameName': 'Restart verification'}, token=white)['gameID']
    for token, color in [(white, 'WHITE'), (black, 'BLACK')]:
        request('PUT', '/game', body={'gameID': game_id, 'playerColor': color}, token=token)
    expected = {'games': [{'gameID': game_id, 'whiteUsername': 'white',
                          'blackUsername': 'black', 'gameName': 'Restart verification'}]}
    assert request('GET', '/game', token=white_login) == expected
    request('DELETE', '/session', token=white)

    server.restart()
    assert request('GET', '/game', token=white_login) == expected
    assert request('GET', '/game', token=black) == expected
    request('GET', '/game', expected=401, token=white)
    request('POST', '/session', body={'username': 'white', 'password': 'white-test-password'})
    next_id = request('POST', '/game', body={'gameName': 'After restart'}, token=black)['gameID']
    assert next_id > game_id
    request('DELETE', '/session', token=white_login)
    print('PASS: credentials, independent sessions, player seats, games, and generated IDs survive restart')

    server.restart()
    request('GET', '/game', expected=401, token=white)
    request('GET', '/game', expected=401, token=white_login)
    assert len(request('GET', '/game', token=black)['games']) == 2
    print('PASS: logout survives another JVM restart without revoking the other user')
    request('DELETE', '/db')

    server.restart()
    request('GET', '/game', expected=401, token=black)
    request('POST', '/session', expected=401,
            body={'username': 'white', 'password': 'white-test-password'})
    new_token = request('POST', '/user', body={
        'username': 'white', 'password': 'replacement-password', 'email': 'new@example.test'})['authToken']
    assert request('GET', '/game', token=new_token) == {'games': []}
    print('PASS: clear removes users, sessions, and games across restart')


def drop_catalog(java, jar, directory, catalog):
    # Use the packaged JDBC driver/config rather than requiring a MySQL CLI or
    # putting credentials into command-line arguments. Check ownership before DROP.
    cleanup = directory / 'Cleanup.java'
    cleanup.write_text('''
import dataaccess.DatabaseManager;
public class Cleanup {
    public static void main(String[] args) throws Exception {
        if (!args[0].matches("chess_package_[0-9a-f]{32}")) {
            throw new IllegalArgumentException("Unexpected verification catalog");
        }
        DatabaseManager.createDatabase();
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.createStatement()) {
            if (!args[0].equals(connection.getCatalog())) {
                throw new IllegalStateException("Refusing to drop another catalog");
            }
            statement.executeUpdate("DROP DATABASE `" + args[0] + "`");
        }
    }
}
''')
    subprocess.run([java, '--class-path', str(jar), str(cleanup), catalog], check=True, timeout=30)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, default=ROOT / 'server/target/server-test-dependencies.jar')
    parser.add_argument('--config', type=Path, default=ROOT / 'server/src/main/resources/db.properties')
    args = parser.parse_args()
    java_home = os.environ.get('JAVA_HOME')
    java = str(Path(java_home) / 'bin/java') if java_home else shutil.which('java')
    if not java:
        raise RuntimeError('Java 21 is required')
    if not args.jar.is_file() or not args.config.is_file():
        raise FileNotFoundError('Build the server JAR and create local db.properties first')
    catalog = 'chess_package_' + uuid.uuid4().hex
    with tempfile.TemporaryDirectory(prefix='chess-package-') as temporary:
        directory = Path(temporary)
        jar = directory / 'server.jar'
        isolated_jar(args.jar, args.config, jar, catalog)
        server = ServerProcess(java, jar, directory)
        try:
            verify_restarts(server)
        finally:
            server.stop()
            drop_catalog(java, jar, directory, catalog)
    print('PASS: four JVM launches completed; disposable catalog and temporary files removed')


if __name__ == '__main__':
    main()
