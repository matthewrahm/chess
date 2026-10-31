#!/usr/bin/env python3
"""Run tests in a copied checkout with MySQL access limited to one disposable DB.

Requires a local MySQL administrator (CLI option files may supply credentials),
Maven, and JAVA_HOME. The checkout's db.properties and build outputs are untouched.
"""

import argparse
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET


def arguments():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mysql", default="mysql", help="MySQL CLI executable")
    parser.add_argument("--admin-user", default="root")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--output", type=Path, required=True, help="New directory for logs and reports")
    parser.add_argument("--grader-libs", type=Path, help="Run the public grader's JUnit/JaCoCo tools on dataaccess only")
    parser.add_argument("--grader-java", default="java")
    return parser.parse_args()


def copy_sources(root, destination):
    files = subprocess.check_output(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"], cwd=root
    ).decode().split("\0")
    for name in set(files) - {""}:
        source = root / name
        if source.is_file():
            target = destination / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)


def save_reports(checkout, output):
    totals = [0, 0, 0, 0]
    for module in ("shared", "server", "client"):
        reports = checkout / module / "target/surefire-reports"
        if reports.exists():
            shutil.copytree(reports, output / module / "surefire-reports")
            for report in reports.glob("TEST-*.xml"):
                suite = ET.parse(report).getroot()
                for i, field in enumerate(("tests", "failures", "errors", "skipped")):
                    totals[i] += int(suite.get(field, "0"))
    print("Tests: {}; failures: {}; errors: {}; skipped: {}".format(*totals))
    coverage = checkout / "server/target/site/jacoco"
    if coverage.exists():
        shutil.copytree(coverage, output / "jacoco")
        report = ET.parse(coverage / "jacoco.xml").getroot()
        package = report.find("package[@name='dataaccess/sql']")
        if package is not None:
            lines = package.find("counter[@type='LINE']")
            covered, missed = int(lines.get("covered")), int(lines.get("missed"))
            print(f"SQL line coverage: {covered}/{covered + missed} ({covered / (covered + missed):.2%})")


def run_grader_tools(checkout, args):
    libs = args.grader_libs.resolve()
    jar = checkout / "server/target/server-test-dependencies.jar"
    classes = checkout / "server/target/test-classes"
    with (args.output / "junit.log").open("w") as log:
        result = subprocess.run([
            args.grader_java, f"-javaagent:{libs / 'jacocoagent.jar'}", "-jar",
            str(libs / "junit-platform-console-standalone-1.10.1.jar"), "execute",
            "--class-path", f"{classes}:{jar}:{libs / 'junit-jupiter-api-5.10.1.jar'}",
            "--details=summary", "--reports-dir=test-output", "-p", "dataaccess",
        ], cwd=checkout, stdout=log, stderr=subprocess.STDOUT)
    shutil.copytree(checkout / "test-output", args.output / "grader-tests")
    subprocess.run([
        args.grader_java, "-jar", str(libs / "jacococli.jar"), "report", "jacoco.exec",
        "--classfiles", str(checkout / "server/target/classes"),
        "--csv", str(args.output / "grader-coverage.csv"),
        "--xml", str(args.output / "grader-coverage.xml"),
    ], cwd=checkout, check=True, stdout=subprocess.DEVNULL)
    print(f"Public grader tools exit: {result.returncode}; dataaccess tests only.")
    return result.returncode


def main():
    args = arguments()
    args.output = args.output.resolve()
    args.output.mkdir(parents=True, exist_ok=False)
    root = Path(__file__).resolve().parents[1]
    suffix = secrets.token_hex(10)
    database = "chessverify" + suffix
    user = "chesscheck" + suffix
    password = secrets.token_hex(24)
    admin = [args.mysql, "--host", args.host, "--port", str(args.port), "--user", args.admin_user]

    def admin_sql(sql):
        subprocess.run(admin, input=sql, text=True, check=True, stdout=subprocess.DEVNULL)

    try:
        admin_sql(f"CREATE USER '{user}'@'%' IDENTIFIED BY '{password}';"
                  f"GRANT ALL ON `{database}`.* TO '{user}'@'%';")
        restricted_env = dict(os.environ, MYSQL_PWD=password)
        denial = subprocess.run(
            [args.mysql, "--host", args.host, "--port", str(args.port), "--user", user],
            input=f"CREATE DATABASE `{database}outside`;", text=True, env=restricted_env,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        )
        if denial.returncode == 0 or "ERROR 1044" not in denial.stderr:
            raise RuntimeError("Expected database access denial was not reproduced")
        print("Confirmed: test account cannot create a database outside its assigned catalog.", flush=True)
        with tempfile.TemporaryDirectory(prefix="chess-verify-") as temporary:
            checkout = Path(temporary)
            copy_sources(root, checkout)
            config = checkout / "server/src/main/resources/db.properties"
            config.parent.mkdir(parents=True, exist_ok=True)
            config.write_text(f"db.name={database}\ndb.host={args.host}\ndb.port={args.port}\n"
                              f"db.user={user}\ndb.password={password}\n")
            command = ["mvn", "-B", "clean", "verify"]
            if args.grader_libs:
                command = ["mvn", "-B", "-DskipTests", "clean", "package"]
            with (args.output / "maven.log").open("w") as log:
                result = subprocess.run(command, cwd=checkout,
                                        stdout=log, stderr=subprocess.STDOUT)
            if args.grader_libs and result.returncode == 0:
                return run_grader_tools(checkout, args)
            save_reports(checkout, args.output)
            print(f"Maven exit: {result.returncode}; reports: {args.output}")
            return result.returncode
    finally:
        admin_sql(f"DROP DATABASE IF EXISTS `{database}`;"
                  f"DROP DATABASE IF EXISTS `{database}outside`;"
                  f"DROP USER IF EXISTS '{user}'@'%';")
        print("Removed temporary database and account; original checkout unchanged.")


if __name__ == "__main__":
    raise SystemExit(main())
