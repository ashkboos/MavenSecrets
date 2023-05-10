from urllib.parse import urlparse
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
    print("Connected")

    parse_url("scm_url", conn)
    parse_url('homepage_url', conn)

    cur = conn.cursor()
    print('*' * 50)
    cur.execute('SELECT COUNT(*) FROM packages WHERE scm_url IS NULL')
    print('scm_url is NULL =', cur.fetchall()[0][0])
    cur.execute('SELECT COUNT(*) FROM packages WHERE homepage_url IS NULL')
    print('homepage_url is NULL =', cur.fetchall()[0][0])


def parse_url(field: str, conn):
    cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)
    cur.execute(
        'SELECT id, {0} FROM packages WHERE {0} IS NOT NULL ORDER BY {0}'.format(field))

    hosts = []
    unparseable = []

    for record in cur:
        url = record[field]

        if field == 'scm_url':
            host = parse_git_url(url)
        elif field == 'homepage_url':
            host = parse_homepage_url(url)

        if host:
            hosts.append(host)
        else:
            unparseable.append(host)

    print('*' * 50)
    print(field)
    print([(host, hosts.count(host)) for host in set(hosts)])
    print('Parsed =', len(hosts))
    print('NOT Parsed =', len(unparseable))
    return hosts, unparseable


def parse_git_url(url):
    n_url = re.sub(r"^(scm|svn):git:", "", url)
    n_url = re.sub(r"^(scm|svn):", "", n_url)
    n_url = re.sub(r"^git@", "ssh://git@", n_url)
    n_url = re.sub(r"^git:", "", n_url)
    n_url = re.sub(r"^(ssh://[^@]+@[^:]+):", r"\1/", n_url)

    parsed_url = urlparse(n_url)
    if parsed_url.hostname == None:
        print(url)
    return parsed_url.hostname


def parse_homepage_url(url):
    parsed_url = urlparse(url)
    if parsed_url.hostname == None:
        print(url)
    return parsed_url.hostname


main()
