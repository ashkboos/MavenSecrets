from urllib.parse import urlparse

from database import *
import re

# Scenarios
# 1. ONLY scm_url
# 2. ONLY homepage_url
# 3. scm_url & homepage_url are the same
# 3. scm_url & homepage_url are different
# 4. BOTH null



class Extractor:

    def __init__(self, db: Database):
        self.db = db


    def extract(self) -> None:
        self.db.create_table()
        unparseable: list = self.process_url("scm_url")
        print(*unparseable, sep='\n')
        # self.process_url('homepage_url', cur)


    # every 100 records, insert the hostnames into new database
    def process_url(self, field: str) -> list:
        # TODO make these into a class
        groupids = []
        artifactids = []
        versions = []
        urls = []
        hosts = []
        unparseable = []

        records = self.db.get_distinct_urls('scm_url')

        # if not enough memory, look into server-side cursors
        for record in records:
            groupid = record['groupid']
            artifactid = record['artifactid']
            version = record['version']
            url = record[field]

            if field == 'scm_url':
                host = self.parse_git_url(url)
            elif field == 'homepage_url':
                host = self.parse_homepage_url(url)

            if host:
                urls.append(url)
                hosts.append(host)
                groupids.append(groupid)
                artifactids.append(artifactid)
                versions.append(version)
            else:
                unparseable.append(url)

            if (len(hosts) == 100):
                self.db.insert_hosts(groupids, artifactids,
                                     versions, urls, hosts)
                groupids.clear()
                artifactids.clear()
                versions.clear()
                urls.clear()
                hosts.clear()

        # TODO final request
        if (len(hosts) > 0):
            self.db.insert_hosts(groupids, artifactids,
                                 versions, urls, hosts)

        print('*' * 50)
        print(field)
        return unparseable


    def parse_git_url(self, url) -> str:
        n_url = re.sub(r"^(scm|svn):git:", "", url)
        n_url = re.sub(r"^(scm|svn):", "", n_url)
        n_url = re.sub(r"^git@", "ssh://git@", n_url)
        n_url = re.sub(r"^git:", "", n_url)
        n_url = re.sub(r"^(ssh://[^@]+@[^:]+):", r"\1/", n_url)

        parsed_url = urlparse(n_url)
        return parsed_url.hostname


    def parse_homepage_url(self, url) -> str:
        parsed_url = urlparse(url)
        return parsed_url.hostname
