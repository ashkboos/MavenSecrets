from giturlparse import parse
from database import *

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

    # cur = conn.cursor()
    # cur.execute('SELECT COUNT(*) FROM packages WHERE scm_url IS NULL')
    # print('scm_url is NULL =', cur.fetchall()[0][0])


def parse_url(field: str, conn):
    cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)
    cur.execute(
        'SELECT id, {0} FROM packages WHERE {0} IS NOT NULL ORDER BY {0}'.format(field))

    hosts = []
    unparseable = []

    for record in cur:
        url = record[field]

        try:
            parsed_url = parse(url)
            host = parsed_url.host
            hosts.append(host)
        except:
            unparseable.append(url)

    print('*' * 50)
    print(field)
    print(sorted([(host, hosts.count(host)) for host in set(hosts)]))
    print('Parsed =', len(hosts))
    print('NOT Parsed =', len(unparseable))


main()
