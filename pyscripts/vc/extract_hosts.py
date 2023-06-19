from urllib.parse import urlparse
import logging
import re

from database import Database
from common.config import Config
from common.packageId import PackageId


# Scenarios
# 1. ONLY scm_url
# 2. ONLY homepage_url
# 3. scm_url & homepage_url are the same
# 3. scm_url & homepage_url are different
# 4. BOTH null


class Extractor:
    def __init__(self, db: Database, config: Config):
        self.log = logging.getLogger(__name__)
        self.db = db
        self.config = config

    # TODO save the unparseable to an error table

    def extract(self) -> None:
        self.db.create_err_table()
        self.db.create_table()
        self.process_urls("scm_url")
        self.process_urls("homepage_url")
        self.process_urls("dev_conn_url")
        self.process_urls("scm_conn_url")

    def process_urls(self, field: str):
        self.log.info(f"Parsing hosts for {field} field")
        # TODO make these into a class
        groupids = []
        artifactids = []
        versions = []
        urls = []
        hosts = []
        unparseable = []

        records = self.db.get_urls(field)

        # if not enough memory, look into server-side cursors
        for record in records:
            pkg = PackageId(record["groupid"], record["artifactid"], record["version"])
            url = record[field]

            host = self.parse_git_url(url)

            if host:
                urls.append(url)
                hosts.append(host)
                groupids.append(pkg.groupid)
                artifactids.append(pkg.artifactid)
                versions.append(pkg.version)
            else:
                unparseable.append(url)
                self.db.insert_error(pkg, url, f"(EXTRACTOR) Couldn't parse")

            # every 1000 records, insert the hostnames into new database
            if len(hosts) == 1000:
                self.db.insert_hosts(
                    groupids, artifactids, versions, urls, hosts, field
                )
                groupids.clear()
                artifactids.clear()
                versions.clear()
                urls.clear()
                hosts.clear()

        if len(hosts) > 0:
            self.db.insert_hosts(groupids, artifactids, versions, urls, hosts, field)

    def parse_git_url(self, url: str) -> str:
        n_url = re.sub(r"^(scm|svn):git:", "", url)
        n_url = re.sub(r"^(scm|svn):", "", n_url)
        n_url = re.sub(r"^git@", "ssh://git@", n_url)
        n_url = re.sub(r"^git:", "", n_url)
        n_url = re.sub(r"^(ssh://[^@]+@[^:]+):", r"\1/", n_url)

        parsed_url = urlparse(n_url)
        return parsed_url.hostname

    def parse_homepage_url(self, url: str) -> str:
        parsed_url = urlparse(url)
        return parsed_url.hostname
