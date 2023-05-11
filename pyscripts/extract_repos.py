from urllib.parse import urlparse

from psycopg2.extensions import cursor
from psycopg2.extras import DictCursor, execute_batch
from database import *
import re

# Scenarios
# 1. ONLY scm_url
# 2. ONLY homepage_url
# 3. scm_url & homepage_url are the same
# 4. BOTH null


def main():
    db = Database('localhost', '5432', 'postgres', 'SuperSekretPassword')
    conn = db.connect()
    cur = conn.cursor(cursor_factory=DictCursor)
    print("Connected")

    create_table(cur)
    process_url("scm_url", cur)
    process_url('homepage_url', cur)

    print('*' * 50)
    cur.execute('SELECT COUNT(*) FROM packages WHERE scm_url IS NULL')
    print('scm_url is NULL =', cur.fetchall()[0][0])
    cur.execute('SELECT COUNT(*) FROM packages WHERE homepage_url IS NULL')
    print('homepage_url is NULL =', cur.fetchall()[0][0])

    # cleanup
    cur.close()
    conn.close()


# every 100 records, insert the hostnames into new database
def process_url(field: str, cur: DictCursor):
    cur.execute(
        'SELECT id, {0} FROM packages WHERE {0} IS NOT NULL ORDER BY {0}'.format(field))
    
    urls = []
    hosts = []
    unparseable = []

    for record in cur:
        url = record[field]

        if field == 'scm_url':
            host = parse_git_url(url)
        elif field == 'homepage_url':
            host = parse_homepage_url(url)

        if host:
            urls.append(url)
            hosts.append(host)
        else:
            unparseable.append(host)

        if (len(hosts) == 100):
            insert_hosts(urls, hosts, cur)
            urls.clear()
            hosts.clear()

    # TODO final request

    print('*' * 50)
    print(field)
    # print([(host, hosts.count(host)) for host in set(hosts)])
    # TODO make query to fetch all counts
    print('Parsed =', len(hosts))
    print('NOT Parsed =', len(unparseable))
    return hosts, unparseable


def parse_git_url(url) -> str:
    n_url = re.sub(r"^(scm|svn):git:", "", url)
    n_url = re.sub(r"^(scm|svn):", "", n_url)
    n_url = re.sub(r"^git@", "ssh://git@", n_url)
    n_url = re.sub(r"^git:", "", n_url)
    n_url = re.sub(r"^(ssh://[^@]+@[^:]+):", r"\1/", n_url)

    parsed_url = urlparse(n_url)
    if parsed_url.hostname == None:
        print(url)
    return parsed_url.hostname


def parse_homepage_url(url) -> str:
    parsed_url = urlparse(url)
    if parsed_url.hostname == None:
        print(url)
    return parsed_url.hostname


def insert_hosts(urls: list, hostnames: list, cur: DictCursor):
    query = "INSERT INTO hosts (url, hostname) VALUES (%s, %s)"
    data = list(zip(urls, hostnames))
    execute_batch(cur, query, data)

def create_table(cur: DictCursor):
    query = '''
    CREATE TABLE IF NOT EXISTS hosts(
        url VARCHAR,
        hostname VARCHAR
    )
    '''
    cur.execute(query)

main()
