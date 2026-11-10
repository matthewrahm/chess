#!/usr/bin/env python3
"""Exercise packaged pregame clients against a real server and disposable MySQL DB.

Requires built client/server JARs, Java 21, port 8080 free, and a local database
account allowed to create a temporary catalog. Development data is never cleared.
"""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import uuid

from verify_packaged_server import ROOT, ServerProcess, drop_catalog, isolated_jar, request

PASSWORD = 'package-rehearsal-secret'
ANSI = re.compile(r'\x1b\[[0-9;]*m')


class ClientRunner:
    def __init__(self, java, jar, output):
        self.java = java
        self.jar = jar
        self.output = output

    def run(self, name, commands, arguments=('8080',)):
        result = subprocess.run([self.java, '-jar', str(self.jar), *arguments],
                                input=commands, text=True, encoding='utf-8',
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=30)
        text = result.stdout
        (self.output / f'{name}.txt').write_text(text, encoding='utf-8')
        assert result.returncode == 0, f'{name}: client exited unsuccessfully'
        assert PASSWORD not in text, f'{name}: password appeared in output'
        assert not any(value in text for value in ('Exception', 'authToken', '"gameID"')), name
        return ANSI.sub('', text)


def require(text, *messages):
    for message in messages:
        assert message in text, f'Missing expected client message: {message}'


def registration(name):
    return f'register\n{name}\n{PASSWORD}\n{name}@example.test\n'


def login(name):
    return f'login\n{name}\n{PASSWORD}\n'


def board(text, color, count=1):
    files = 'abcdefgh' if color == 'WHITE' else 'hgfedcba'
    border = '   ' + ''.join(f' {file} ' for file in files) + '   '
    assert text.splitlines().count(border) == count * 2, f'Incorrect {color} board borders'
    top_rank, pieces = (8, 'rnbqkbnr') if color == 'WHITE' else (1, 'RNBKQBNR')
    top = f' {top_rank} ' + ''.join(f' {piece} ' for piece in pieces) + f' {top_rank} '
    assert text.splitlines().count(top) == count, f'Incorrect {color} top rank'


def verify_players(client):
    # Make the eventual game's internal ID differ from its displayed list number.
    token = request('POST', '/user', body={
        'username': 'seed', 'password': PASSWORD, 'email': 'seed@example.test'})['authToken']
    request('POST', '/game', body={'gameName': 'Discarded'}, token=token)
    request('DELETE', '/db')
    white = client.run('white', registration('white') +
                       'create\nPractice\nlist\nplay\nabc\nplay\n1\ngreen\n'
                       'play\n1\nwhite\nredraw\nback\nlogout\nquit\n')
    require(white, 'Game created.', '1. Practice | White: (open) | Black: (open)',
            'Please enter a listed game number.', 'Please choose white or black.',
            'Joined as white.', 'Returned to the lobby.', 'Signed out.', 'Goodbye!')
    board(white, 'WHITE', count=2)
    black = client.run('black', registration('black') +
                       'play\n1\nwhite\nplay\n1\nblack\nback\nlist\nlogout\nquit\n')
    require(black, 'That seat is already taken.', 'Joined as black.',
            '1. Practice | White: white | Black: black', 'Signed out.', 'Goodbye!')
    board(black, 'BLACK')
    token = request('POST', '/session', body={'username': 'white', 'password': PASSWORD})['authToken']
    games = request('GET', '/game', token=token)
    assert len(games['games']) == 1, 'Client created an unexpected number of games'
    game = games['games'][0]
    assert game['gameID'] > 1, 'Game ID must differ from display number for this check'
    assert game['whiteUsername'] == 'white' and game['blackUsername'] == 'black'
    assert game['gameName'] == 'Practice'
    return token, games


def verify_observer(client, token, expected):
    observer = client.run('observer', registration('observer') +
                          'observe\n1\nback\nlogout\nlogin\nobserver\nincorrect\n' +
                          login('observer') + 'observe\n1\nback\nlogout\nquit\n')
    require(observer, 'Observing the initial position.', 'Incorrect username or password.',
            'Signed in as observer.', 'Goodbye!')
    assert 'Joined as' not in observer, 'Observation must not join a seat'
    board(observer, 'WHITE', count=2)
    assert request('GET', '/game', token=token) == expected, 'Observation changed the game'


def verify_restart_and_outage(server, client, token, expected):
    server.restart()
    assert request('GET', '/game', token=token) == expected, 'Game or session did not survive restart'
    restored = client.run('after-restart', login('white') + 'list\nobserve\n1\nback\nlogout\nquit\n')
    require(restored, 'Signed in as white.', '1. Practice | White: white | Black: black', 'Signed out.')
    board(restored, 'WHITE')
    server.stop()
    offline = client.run('server-offline', login('white') + 'help\nquit\n')
    require(offline, 'Unable to reach the server. Please try again.', '[SIGNED OUT] >>>', 'Goodbye!')
    assert '[SIGNED IN] >>>' not in offline
    for index, arguments in enumerate([('bad',), ('0',), ('65536',), ('8080', 'extra')]):
        invalid = client.run(f'invalid-arguments-{index}', '', arguments)
        require(invalid, 'Usage:', 'Choose a port from 1 to 65535.')


def arguments():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--client-jar', type=Path, default=ROOT / 'client/target/client-test-dependencies.jar')
    parser.add_argument('--server-jar', type=Path, default=ROOT / 'server/target/server-test-dependencies.jar')
    parser.add_argument('--config', type=Path, default=ROOT / 'server/src/main/resources/db.properties')
    parser.add_argument('--output', type=Path, required=True, help='New directory for client transcripts')
    return parser.parse_args()


def main():
    args = arguments()
    java_home = os.environ.get('JAVA_HOME')
    java = str(Path(java_home) / 'bin/java') if java_home else shutil.which('java')
    if not java:
        raise RuntimeError('Java 21 is required')
    for path in (args.client_jar, args.server_jar, args.config):
        if not path.is_file():
            raise FileNotFoundError('Build both JARs and create local db.properties first')
    args.output.mkdir(parents=True, exist_ok=False)
    catalog = 'chess_package_' + uuid.uuid4().hex
    with tempfile.TemporaryDirectory(prefix='chess-client-package-') as temporary:
        directory = Path(temporary)
        jar = directory / 'server.jar'
        isolated_jar(args.server_jar, args.config, jar, catalog)
        server = ServerProcess(java, jar, directory)
        client = ClientRunner(java, args.client_jar.resolve(), args.output)
        try:
            server.start()
            token, expected = verify_players(client)
            print('PASS: create/list/play, invalid selections, occupied seats, both board perspectives')
            verify_observer(client, token, expected)
            print('PASS: full-game observation preserves seats, wrong-password retry and logout')
            verify_restart_and_outage(server, client, token, expected)
            print('PASS: persistent game/session, fresh client login, server outage and invalid startup arguments')
        finally:
            server.stop()
            drop_catalog(java, jar, directory, catalog)
    (args.output / 'result.txt').write_text('PASS: all packaged-client checks; disposable catalog removed.\n')
    print('PASS: all packaged-client checks; disposable catalog and temporary files removed')


if __name__ == '__main__':
    main()
