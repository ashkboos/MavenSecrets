import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed
import logging
from time import sleep
from typing import Dict

from database import Database
from packageId import PackageId
from config import Config
from utils import *


class VerifyHost:
    def __init__(self, db: Database, config: Config):
        self.db = db
        self.config = config
        self.timeout = 15
        self.funcs = [lambda x: (x, True), git_to_https, remove_tree_path]
        self.log = logging.getLogger(__name__)

    def verify_hosts(self):
        self.db.create_err_table()
        records = self.db.get_all_unprocessed()

        with ThreadPoolExecutor() as executor:
            futures = [
                executor.submit(self.verify_single_host, record) for record in records
            ]
            self.log.info("Tasks submitted...")
            for future in as_completed(futures):
                success = future.result()

            self.log.info("All done. Thread Pool shutting down...")

    def verify_single_host(self, record):
        success = False

        urls = [
            record["url"],
            record["url_home"],
            record["url_scm_conn"],
            record["url_dev_conn"],
        ]
        valid_fields = ["valid", "valid_home", "valid_scm_conn", "valid_dev_conn"]

        pkg = PackageId(record["groupid"], record["artifactid"], record["version"])
        errors: Dict[str, str] = {}

        for j, url in enumerate(urls):
            if url is None:
                continue
            if re.match(r"svn\.[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}", url):
                self.log.debug(f"Possible SVN url ignored: {url}")
                continue

            url = remove_scm_prefix(url)
            if re.search("apache.org", url):
                url = convert_link_to_github(url)

            for convert_func in self.funcs:
                converted_url, changed = convert_func(url)
                if not changed:
                    continue

                err = self.try_with(converted_url)
                if err is None:
                    success = True
                    self.db.update_validity(valid_fields[j], pkg, converted_url)
                    self.log.debug(f"VALID: {valid_fields[j]}")
                    break
                else:
                    # TODO only insert errors if all failed
                    errors[converted_url] = err

        if not success:
            for err_url, err in errors.items():
                self.log.critical(err_url)
                self.db.insert_error(pkg, err_url, f'(VERIFIER) {err}')
        self.db.mark_processed(pkg)
        sleep(0.2)
        return success

    def run_cmd(self, url: str):
        return subprocess.run(
            [
                "env",
                "GIT_TERMINAL_PROMPT=0",
                "GIT_SSH_COMMAND=ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no",
                "git",
                "ls-remote",
                "--exit-code",
                url,
                "HEAD",
            ],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=self.timeout,
        )

    def try_with(self, url: str) -> str:
        try:
            self.log.debug(f"Trying with {url}")
            process = self.run_cmd(url)
        except subprocess.TimeoutExpired:
            self.log.error(f"TIMED OUT after {self.timeout}s!")
            return "TIMED OUT"

        output = process.stdout.decode()
        err = process.stderr.decode()
        print(output)
        self.log.debug(f"Exit code: {process.returncode}")
        if process.returncode == 0:
            return None
        else:
            return f'({process.returncode}):{err}'


# Exceptions:

# git@gitee.com:fluent-mybatis/generator.git DOESNT WORK
# BUT
# https://gitee.com/fluent-mybatis/generator.git WORKS

# https://github.com/Auties00/noise-java/tree/master/ DOESNT WORK
# BUT
# https://github.com/Auties00/noise-java/ WORKS

# http needs to be transformed to https, otherwise github rejects

# scm:git:git@github.com:jitsni/jcifs.git we need to get rid of scm:git:

# http://github.com:jlangch/venice/tree/master HAS TYPO?

# git://github.com/instaclustr/instaclustr-icarus.git TIMES OUT
# BUT
# https://github.com/instaclustr/instaclustr-icarus.git WORKS

# http://github.com/ericmedvet/jgea/tree/main/jgea.experimenter
# SHOULD remove everything from /tree onwards


# Trying with https://github.com/cerner/ccl-testing/tree/master/ftp-util
# Exit code: 128
# remote: Please upgrade your git client.
# remote: GitHub.com no longer supports git over dumb-http: https://github.com/blog/809-git-dumb-http-transport-to-be-turned-off-in-90-days
# fatal: unable to access 'https://github.com/cerner/ccl-testing/tree/master/ftp-util/': The requested URL returned error: 403

# Ideas:
# - Try scm_url, homepage_url, etc (all regex'd) until something hits
