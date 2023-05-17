from urllib.parse import urlparse

from psycopg2.extensions import connection
from psycopg2.extras import DictCursor, execute_batch
from database import *
import re

# Scenarios
# 1. ONLY scm_url
# 2. ONLY homepage_url
# 3. scm_url & homepage_url are the same
# 4. BOTH null


class Extractor:

    def __init__(self, db) -> None:
        self.SRC_TABLE = 'packages_big'
        self.DEST_TABLE = 'hosts'
        self.conn: connection = db.connect()

    def extract(self):
        cur = self.conn.cursor(cursor_factory=DictCursor)
        print("Connected")

        self.create_table(cur)
        self.conn.commit()

        unparseable: list = self.process_url("scm_url", cur)
        # process_url('homepage_url', cur)
        self.conn.commit()

        # REDO THESE TO CHOOSE DISTINCT VERSIONS
        print('*' * 50)
        cur.execute(f'SELECT COUNT(*) FROM {self.SRC_TABLE} WHERE scm_url IS NULL')
        print('scm_url is NULL =', cur.fetchall()[0][0])
        cur.execute(f'SELECT COUNT(*) FROM {self.SRC_TABLE} WHERE homepage_url IS NULL')
        print('homepage_url is NULL =', cur.fetchall()[0][0])

        # cleanup
        cur.close()
        self.conn.close()

    # every 100 records, insert the hostnames into new database

    def process_url(self, field: str, cur: DictCursor):
        # cur.execute(
        #     'SELECT id, {0} FROM packages_old WHERE {0} IS NOT NULL ORDER BY {0}'.format(field))

        cur.execute(
            f'''
            SELECT DISTINCT ON (split_part(id, ':', 1), split_part(id, ':', 2))
                split_part(id, ':', 1) AS groupid,
                split_part(id, ':', 2) AS artifactid,
                split_part(id, ':', 3) AS version,
                scm_url
            FROM {self.SRC_TABLE}
            WHERE scm_url IS NOT NULL AND scm_url != '';
            '''
        )

        #TODO make this into an class
        groupids = []
        artifactids = []
        versions = []
        urls = []
        hosts = []
        unparseable = []

        # if not enough memory, look into server-side cursors
        for record in cur.fetchall():

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
                unparseable.append(host)

            if (len(hosts) == 100):
                self.insert_hosts(groupids, artifactids, versions, urls, hosts, cur)
                groupids.clear()
                artifactids.clear()
                versions.clear()
                urls.clear()
                hosts.clear()

        # TODO final request

        print('*' * 50)
        print(field)

        print('NOT Parsed =', len(unparseable))
        return unparseable

    def parse_git_url(self, url) -> str:
        n_url = re.sub(r"^(scm|svn):git:", "", url)
        n_url = re.sub(r"^(scm|svn):", "", n_url)
        n_url = re.sub(r"^git@", "ssh://git@", n_url)
        n_url = re.sub(r"^git:", "", n_url)
        n_url = re.sub(r"^(ssh://[^@]+@[^:]+):", r"\1/", n_url)

        parsed_url = urlparse(n_url)
        if parsed_url.hostname == None:
            print(url)
        return parsed_url.hostname

    def parse_homepage_url(self, url) -> str:
        parsed_url = urlparse(url)
        if parsed_url.hostname == None:
            print(url)
        return parsed_url.hostname

    def insert_hosts(self, groupids: list, artifactids: list, versions: list, urls: list, hostnames: list, cur: DictCursor):
        query = f"INSERT INTO {self.DEST_TABLE} (groupid,artifactid,version,url,hostname) VALUES (%s,%s,%s,%s,%s)"
        data = list(zip(groupids, artifactids, versions, urls, hostnames))
        execute_batch(cur, query, data)

    def create_table(self, cur: DictCursor):
        query = f'''
        CREATE TABLE IF NOT EXISTS {self.DEST_TABLE}(
            groupid VARCHAR,
            artifactid VARCHAR,
            version VARCHAR,
            url VARCHAR,
            hostname VARCHAR
        )
        '''
        cur.execute(query)


def main():
    db = Database('localhost', '5432', 'postgres', 'SuperSekretPassword')
    extractor = Extractor(db)
    extractor.extract()


main()
