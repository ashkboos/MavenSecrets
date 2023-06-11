import logging
import re
from time import sleep
from giturlparse import parse
import requests
import subprocess
from datetime import datetime
from typing import Dict

from database import Database
from packageId import PackageId
from config import Config


class GetTags:
    def __init__(self, db: Database, config: Config):
        self.log = logging.getLogger(__name__)
        logging.getLogger("urllib3").setLevel(logging.WARNING)
        self.db = db
        self.config = config
        self.rate_lim_remain = 5000
        self.rate_lim_reset = datetime.utcnow()

    # TODO TRY WITH EACH FIELD UNTIL 1 HITS!
    def find_github_release(self):
        self.db.create_tags_table()

        field = "valid"
        records = self.db.get_valid_github_urls(field)
        for record in records:
            pkg = PackageId(record["groupid"], record["artifactid"], record["version"])
            url = record["url"]

            try:
                p = self.parsePlus(url)
            except Exception as e:
                self.log.error(e)
                self.db.insert_error(pkg, url, f"(GET TAGS) {e}!")
            if p.valid:
                self.log.debug(f"REPO INFO: {p.host}, {p.owner}, {p.name}")
            else:
                self.log.error("Invalid url")
                self.db.insert_error(pkg, url, f"(GET TAGS) Invalid URL!")
                continue

            # TODO retry on rate_limit message (shouldn't happen though)
            try:
                res = self.make_request(p.owner, p.name, pkg.version)
                json = res.json()
                data: Dict = json["data"]
            except Exception as e:
                self.log.exception(e)
                # TODO add to unresolved
                continue

            if res.status_code != 200:
                self.log.error(f"Bad status code received ({res.status_code})!")
                continue

            try:
                self.rate_lim_remain = data["rateLimit"]["remaining"]
                date_string = data["rateLimit"]["resetAt"]
                self.rate_lim_reset = datetime.strptime(
                    date_string, "%Y-%m-%dT%H:%M:%SZ"
                )
                self.log.debug(f"Rate Lim Remaining: {self.rate_lim_remain}")
            except KeyError as e:
                self.log.error("Rate lim response missing!")

            # TODO pagination
            rel_name, rel_tag_name, rel_commit_hash = None, None, None
            tag_name, tag_commit_hash = None, None
            release_exists, tag_exists = False, False
            try:
                releases = data["repository"]["releases"]["nodes"]
                if len(releases) == 100:
                    self.log.warn(
                        "This repo has more than 100 releases. Need to paginate!"
                    )
            except Exception as e:
                self.log.exception(e)
                continue
            if len(releases) > 0:
                matches = [rel for rel in releases if rel.get("name") == pkg.version]
                release_exists = len(matches) > 0

            try:
                tag_exists = len(data["repository"]["refs"]["nodes"]) > 0
            except Exception as e:
                self.log.exception(e)
                self.log.error(
                    f"Repository likely does not exist! Request: ({p.owner},{p.name},{pkg.version})"
                )
                continue

            if release_exists:
                rel_name = matches[0]["name"]
                rel_tag_name = matches[0]["tag"]["name"]
                rel_commit_hash = matches[0]["tagCommit"]["oid"]
                self.log.debug(
                    f"Release {rel_name} with Tag {rel_tag_name} found for Version {pkg.version}!"
                )

            if tag_exists:
                tag_commit_hash = data["repository"]["refs"]["nodes"][0]["target"][
                    "oid"
                ]
                tag_name = data["repository"]["refs"]["nodes"][0]["name"]
                self.log.debug(
                    f"Version {pkg.version} with Tag {tag_name} and commit hash {tag_commit_hash} FOUND!"
                )

            if release_exists or tag_exists:
                self.db.insert_tag(
                    pkg,
                    tag_name,
                    tag_commit_hash,
                    rel_name,
                    rel_tag_name,
                    rel_commit_hash,
                )

            sleep(0.05)

    def build_and_compare(self):
        # TODO replace with only github repos that have a matching tag
        records = self.db.get_all()
        clone_dir = "./clones"
        for record in records:
            url = record["url"]
            process = subprocess.run(["git", "clone", url, clone_dir])
            if process.returncode != 0:
                self.log.error(
                    f"Error encountered when cloning {process.stderr.decode()}"
                )
                continue

    def make_request(self, owner: str, repo: str, version: str):
        self.check_rate_lim()
        token = self.config.GITHUB_API_KEY
        query = """
        query ($owner: String!, $repo: String!, $version: String!) {
          rateLimit {
            cost
            remaining
            resetAt
          }
          repository(owner: $owner, name: $repo) {
            refs(refPrefix: "refs/tags/", query: $version, first: 1) {
              nodes {
                name
                target {
                  oid
                }
              }
            }
            releases(last: 100) {
              nodes {
                name
                tag {
                  name
                }
                tagCommit {
                  oid
                }
              }
            }
          }
        }
        """
        variables = {"owner": owner, "repo": repo, "version": version}
        payload = {"query": query, "variables": variables}
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        }
        res = requests.post(
            "https://api.github.com/graphql", json=payload, headers=headers
        )
        return res

    def check_rate_lim(self):
        if self.rate_lim_remain <= 5:
            timenow = datetime.utcnow()
            sleep_time = (self.rate_lim_reset - timenow).total_seconds()
            self.log.warn(
                f"Waiting for rate limit...\nSleeping for {sleep_time} secs.\nCurrent time: {timenow}\nReset at: {self.rate_lim_reset}"
            )
            sleep(sleep_time + 30)  # +30s to account for possible time desync

    # Replaces http with https, removes trailing slashes
    # and adds .git to git@ urls to make it work with parsing lib
    def parsePlus(self, url: str):
        url = re.sub(r"\/+$", "", url)
        if re.match(r"^git@", url) and not re.search(r"\.git$", url):
            return parse(url + ".git")
        https_url = re.sub(r"http:", "https:", url)
        return parse(https_url)


# exceptions
# org.dispatchhttp,dispatch-all_2.11,0.14.0,v0.14.0-RC1
# org.apache.hudi,hudi-metaserver-server,0.13.0,release-0.13.0-rc1
