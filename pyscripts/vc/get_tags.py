import logging
from time import sleep
import requests
import subprocess
from datetime import datetime
from typing import Dict

from database import Database
from packageId import PackageId
from config import Config
from utils import parse_plus


class GetTags:
    def __init__(self, db: Database, config: Config):
        self.log = logging.getLogger(__name__)
        logging.getLogger("urllib3").setLevel(logging.WARNING)
        self.db = db
        self.config = config
        self.rate_lim_remain = 5000
        self.rate_lim_reset = datetime.utcnow()

    # TODO TRY WITH EACH FIELD UNTIL 1 HITS!
    # TODO MATCH RELEASE USING SEQ. MATCHING ALGO
    def find_github_release(self):
        checkpoint = 0
        field = "valid"

        self.db.create_tags_table()
        records = self.db.get_valid_github_urls(field)
        for record in records:
            rel_name, rel_tag_name, rel_commit_hash = None, None, None
            tag_name, tag_commit_hash = None, None
            release_exists, tag_exists = False, False
            pkg = PackageId(record["groupid"], record["artifactid"], record["version"])
            url = record["url"]

            try:
                repo = parse_plus(url)
            except Exception as e:
                self.log.error(e)
                self.db.insert_error(pkg, url, f"(GET TAGS) {e}!")
            if not repo.valid:
                self.log.error("Invalid url")
                self.db.insert_error(pkg, url, f"(GET TAGS) Invalid URL!")
                continue

            # TODO retry on rate_limit message (shouldn't happen though)
            try:
                res = self.make_request(repo.owner, repo.name, pkg.version)
                json = res.json()
                data: Dict = json["data"]
            except Exception as e:
                self.log.exception(e)
                # TODO add to unresolved
                continue

            if res.status_code != 200:
                self.log.error(f"Bad status code received ({res.status_code})!")
                continue
            self.update_rate_lim(data)

            # Release
            try:
                rel_name, rel_tag_name, rel_commit_hash = self.search_release(
                    data, pkg, repo
                )
            except Exception as e:
                self.log.exception(e)
                continue
            release_exists = rel_name is not None

            # Tag
            try:
                tag_exists = len(data["repository"]["refs"]["nodes"]) > 0
            except Exception as e:
                self.log.exception(e)
                self.log.error(
                    f"Repository likely does not exist! Request: ({repo.owner},{repo.name},{pkg.version})"
                )
                continue

            if tag_exists:
                tag_commit_hash, tag_name = self.extract_tag(data)
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

            checkpoint += 1
            if checkpoint % 1000 == 0:
                self.log.info(f'Checkpoint: Processed {checkpoint} packages...')
            sleep(0.05)

    def search_release(self, data, pkg: PackageId, repo):
        releases: list = data["repository"]["releases"]["nodes"]
        has_next = data["repository"]["releases"]["pageInfo"]["hasNextPage"]
        cursor = data["repository"]["releases"]["pageInfo"]["endCursor"]
        while has_next:
            try:
                res = self.make_request(repo.owner, repo.name, pkg.version, cursor)
                json = res.json()
                data: Dict = json["data"]
                new_releases: list = data["repository"]["releases"]["nodes"]
                has_next = data["repository"]["releases"]["pageInfo"]["hasNextPage"]
                cursor = data["repository"]["releases"]["pageInfo"]["endCursor"]
            except Exception as e:
                self.log.exception(e)
            releases.extend(new_releases)

        if len(releases) == 0:
            return None, None, None
        best_match = self.find_best_match(releases, pkg)
        if best_match is not None:
            return self.extract_release(best_match)
        else:
            return None, None, None

    def find_best_match(self, releases: list, pkg):
        matches = [rel for rel in releases if rel.get("name") == pkg.version]
        if len(matches) > 0:
            return matches[0]
        else:
            return None

    def extract_release(self, release):
        rel_name = release["name"]
        rel_tag_name = release["tag"]["name"]
        rel_commit_hash = release["tagCommit"]["oid"]
        return rel_name, rel_tag_name, rel_commit_hash

    def extract_tag(self, data):
        tag_commit_hash = data["repository"]["refs"]["nodes"][0]["target"]["oid"]
        tag_name = data["repository"]["refs"]["nodes"][0]["name"]
        return (tag_commit_hash, tag_name)

    def make_request(self, owner: str, repo: str, version: str, cursor: str = None):
        self.check_rate_lim()
        token = self.config.GITHUB_API_KEY
        query = """
        query ($owner: String!, $repo: String!, $version: String!, $cursor: String) {
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
            releases(first: 100, after: $cursor, orderBy: {field: CREATED_AT, direction: DESC} ) {
              pageInfo {
                hasNextPage
                endCursor
              }
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
        variables = {
            "owner": owner,
            "repo": repo,
            "version": version,
            "cursor": cursor if cursor else None,
        }
        payload = {"query": query, "variables": variables}
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        }
        res = requests.post(
            "https://api.github.com/graphql", json=payload, headers=headers
        )
        return res

    def update_rate_lim(self, data):
        try:
            self.rate_lim_remain = data["rateLimit"]["remaining"]
            date_string = data["rateLimit"]["resetAt"]
            self.rate_lim_reset = datetime.strptime(date_string, "%Y-%m-%dT%H:%M:%SZ")
            self.log.debug(f"Rate Lim Remaining: {self.rate_lim_remain}")
        except KeyError as e:
            self.log.error("Rate lim response missing!")

    def check_rate_lim(self):
        if self.rate_lim_remain <= 5:
            timenow = datetime.utcnow()
            sleep_time = (self.rate_lim_reset - timenow).total_seconds()
            self.log.info(
                f"Waiting for rate limit...\nSleeping for {sleep_time} secs.\nCurrent time: {timenow}\nReset at: {self.rate_lim_reset}"
            )
            sleep(sleep_time + 30)  # +30s to account for possible time desync


# exceptions
# org.dispatchhttp,dispatch-all_2.11,0.14.0,v0.14.0-RC1
# org.apache.hudi,hudi-metaserver-server,0.13.0,release-0.13.0-rc1

# paging example
# https://github.com/Activiti/Activiti has 289 releases
