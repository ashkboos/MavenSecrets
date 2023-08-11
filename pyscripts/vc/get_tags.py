import logging
from time import sleep
import requests_cache
from datetime import datetime
from typing import Dict

from database import Database
from common.packageId import PackageId
from common.config import Config
from utils import parse_plus


class GetTags:
    def __init__(self, db: Database, config: Config):
        self.log = logging.getLogger(__name__)
        logging.getLogger("urllib3").setLevel(logging.WARNING)
        self.cache = requests_cache.CachedSession(
            "tags_cache", backend="sqlite", allowable_methods=["GET", "POST"]
        )
        self.db = db
        self.config = config
        self.rate_lim_remain = 5000
        self.rate_lim_reset = datetime.utcnow()
        self.update_rate_lim()

    def find_tags(self):
        self.db.create_tags_table()
        records = self.db.get_valid_github_urls()
        checkpoint = 0
        self.log.info(f"Retrieved {len(records)} packages that need to be checked")

        for record in records:
            tag_name, tag_commit_hash = None, None
            tag_exists = False
            sleep(0.01)

            checkpoint += 1
            if checkpoint % 1000 == 0:
                self.log.info(f"Checkpoint: Processed {checkpoint} packages...")
            pkg = PackageId(record["groupid"], record["artifactid"], record["version"])
            urls = [
                record["valid"],
                record["valid_home"],
                record["valid_scm_conn"],
                record["valid_dev_conn"],
            ]
            urls_set = set(urls)
            urls_set.discard(None)
            for url in urls_set:
                try:
                    repo = parse_plus(url)
                    if not repo.valid:
                        self.log.error("Invalid url")
                        self.db.insert_error(pkg, url, f"(GET TAGS) Invalid URL!")
                        continue  # invalid repo, try next URL
                except Exception as e:
                    self.log.error(e)
                    self.db.insert_error(pkg, url, f"(GET TAGS) {e}!")
                    continue  # cannot parse repo owner and name, try next URL

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

                # Tag
                try:
                    tag_name, tag_commit_hash = self.search_tags(data, pkg, repo)
                except Exception as e:
                    self.log.exception(
                        f"Repository likely does not exist! Request: ({repo.owner},{repo.name},{pkg.version})"
                    )
                    continue  # Response doesn't contain all fields, go to next URL

                if tag_name is not None:
                    tag_exists = True
                    self.log.debug(
                        f"Version {pkg.version} with Tag {tag_name} and commit hash {tag_commit_hash} FOUND!"
                    )

                if tag_exists:
                    self.db.insert_tag(
                        pkg,
                        url,
                        tag_name,
                        tag_commit_hash,
                    )
                    break  # Don't try with the other URLS, go to next package
                else:
                    self.db.insert_tag(pkg)

    def make_request(self, owner: str, repo: str, version: str, cursor: str = None):
        """Makes a request to the Github GraphQL API to obtain the tags for a
        specific repository from newest to oldest. Also obtains the current rate
        limit to cut down on requests. The cursor is used for pagination.
        """
        self.check_rate_lim()
        self.log.debug(f"Making request for {owner}:{repo}:{version}, cursor={cursor}")
        token = self.config.GITHUB_API_KEY
        query = """
        query ($owner: String!, $repo: String!, $cursor: String) {
          rateLimit {
            cost
            remaining
            resetAt
          }
          repository(owner: $owner, name: $repo) {
            refs(refPrefix: "refs/tags/", first: 100, after: $cursor, orderBy: {field: TAG_COMMIT_DATE, direction: DESC}) {
              nodes {
                name
                target {
                  oid
                }
              }
              pageInfo {
                hasNextPage
                endCursor
              }
            }
          }
        }
        """
        variables = {
            "owner": owner,
            "repo": repo,
            "cursor": cursor if cursor else None,
        }
        payload = {"query": query, "variables": variables}
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        }
        res = self.cache.post(
            "https://api.github.com/graphql", json=payload, headers=headers
        )
        if res.from_cache:
            self.log.debug("Ignoring rate limit info from cache.")
            self.log.debug(f"Rate Lim Remaining: {self.rate_lim_remain}")
        else:
            self.update_rate_lim(res)
        return res

    def update_rate_lim(self, res: requests_cache.Response = None):
        """Given a Github API response containing the rate limit info, updates the
        rate limit state locally. If no response is given, it queries the Github API
        to update.
        """
        if res is None:
            query = """
            query { 
              rateLimit {
                resetAt
                remaining
              }
            }
            """
            payload = {"query": query}
            headers = {
                "Authorization": f"Bearer {self.config.GITHUB_API_KEY}",
                "Content-Type": "application/json",
            }
            res = self.cache.post(
                "https://api.github.com/graphql",
                json=payload,
                headers=headers,
                expire_after=requests_cache.DO_NOT_CACHE,
            )

        data = res.json()["data"]
        try:
            self.rate_lim_remain = data["rateLimit"]["remaining"]
            date_string = data["rateLimit"]["resetAt"]
            self.rate_lim_reset = datetime.strptime(date_string, "%Y-%m-%dT%H:%M:%SZ")
            self.log.debug(f"Rate Lim Remaining: {self.rate_lim_remain}")
        except Exception as e:
            self.log.exception(f"Rate lim response missing!\nData:{data}")

    def check_rate_lim(self):
        """[BLOCKING] Checks whether the rate limit has been reached. If yes,
        sleep until the rate limit reset time.
        """
        while self.rate_lim_remain <= 5:
            timenow = datetime.utcnow()
            sleep_time = (self.rate_lim_reset - timenow).total_seconds()
            self.log.info(
                f"Waiting for rate limit...\nSleeping for {sleep_time} secs.\nCurrent time: {timenow}\nReset at: {self.rate_lim_reset}"
            )
            if sleep_time > 0:
                sleep(sleep_time + 30)  # +30s to account for possible time desync
            else:
                sleep(10)
            self.update_rate_lim()

    def search_tags(self, data, pkg: PackageId, repo):
        """Given the API response, searches for the tag corresponding
        to the version on pkg.version.

        Parameters:
            data: Github GraphQL API response
            pkg: the package
            repo: repository metadata

        Returns:
            Tuple containing (tag name, tag commit hash)
        """
        tags: list = data["repository"]["refs"]["nodes"]
        has_next = data["repository"]["refs"]["pageInfo"]["hasNextPage"]
        cursor = data["repository"]["refs"]["pageInfo"]["endCursor"]
        while has_next:
            try:
                res = self.make_request(repo.owner, repo.name, pkg.version, cursor)
                json = res.json()
                data: Dict = json["data"]
                more_tags: list = data["repository"]["refs"]["nodes"]
                has_next = data["repository"]["refs"]["pageInfo"]["hasNextPage"]
                cursor = data["repository"]["refs"]["pageInfo"]["endCursor"]
            except Exception as e:
                self.log.exception(e)
            tags.extend(more_tags)
        if len(tags) > 0:
            best_match = self.find_best_match_tag(tags, pkg)
            if best_match is not None:
                return best_match["name"], best_match["target"]["oid"]
        return None, None

    def find_best_match_tag(self, tags: list, pkg: PackageId):
        """Given a list of tags, finds the tag that matches
        the fixed tag scheme. Since there *could* be multiple
        matches, we return the latest one.

        Parameters:
            tags: list of tags extracted from response
            pkg: the package

        Returns:
            the latest tag object matching the fixed tag scheme
        """
        artifactid, version = pkg.artifactid, pkg.version
        artifact_parts = artifactid.split("-")
        possible_tags: list[str] = [
            version,
            artifactid + "-" + version,
            "version-" + version,
            "v" + version,
            "v." + version,
            "release-" + version,
            "release-v" + version,
            "release_" + version,
            "release_v" + version,
            "release/" + version,
            "release/v" + version,
            "releases/" + version,
            "rel-" + version,
            "rel_" + version,
            "rel_v" + version,
            "rel/" + version,
            "rel/v" + version,
            "r" + version,
            "r." + version,
            "project-" + version,
            version + "-release",
            version + ".release",
            "v" + version + ".release",
            version + ".final",
            version + "-final",
            "v" + version + "-final",
            "tag-" + version,
            "tag" + version,
            artifact_parts[0] + "-" + version,
            artifact_parts[0] + "-v" + version,
            # TODO Add all of the cases!!!
        ]
        possible_tags = [tag.lower() for tag in possible_tags]
        filtered = list(filter(lambda tag: tag["name"].lower() in possible_tags, tags))
        if len(filtered) > 1:
            self.log.debug(f"Found multiple matching tags: {filtered}")
        if filtered:
            return filtered[0]  # return the first one (most recent)
        else:
            return None
