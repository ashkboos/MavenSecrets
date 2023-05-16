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
        self.conn: connection = db.connect()

    def extract(self):
        cur = self.conn.cursor(cursor_factory=DictCursor)
        print("Connected")

        self.create_table(cur)
        self.conn.commit()

        unparseable: list = self.process_url("scm_url", cur)
        # process_url('homepage_url', cur)
        self.conn.commit()

        print('*' * 50)
        cur.execute('SELECT COUNT(*) FROM packages WHERE scm_url IS NULL')
        print('scm_url is NULL =', cur.fetchall()[0][0])
        cur.execute('SELECT COUNT(*) FROM packages WHERE homepage_url IS NULL')
        print('homepage_url is NULL =', cur.fetchall()[0][0])

        # cleanup
        cur.close()
        self.conn.close()

    # every 100 records, insert the hostnames into new database

    def process_url(self, field: str, cur: DictCursor):
        cur.execute(
            'SELECT id, {0} FROM packages_old WHERE {0} IS NOT NULL ORDER BY {0}'.format(field))

        urls = []
        hosts = []
        unparseable = []

        # if not enough memory, look into server-side cursors
        for record in cur.fetchall():
            url = record[field]

            if field == 'scm_url':
                host = self.parse_git_url(url)
            elif field == 'homepage_url':
                host = self.parse_homepage_url(url)

            if host:
                urls.append(url)
                hosts.append(host)
            else:
                unparseable.append(host)

            if (len(hosts) == 100):
                self.insert_hosts(urls, hosts, cur)
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

    def insert_hosts(self, urls: list, hostnames: list, cur: DictCursor):
        query = "INSERT INTO hosts (url, hostname) VALUES (%s, %s)"
        data = list(zip(urls, hostnames))
        execute_batch(cur, query, data)

    def create_table(self, cur: DictCursor):
        query = '''
        CREATE TABLE IF NOT EXISTS hosts(
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
