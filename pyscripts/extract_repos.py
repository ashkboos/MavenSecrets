from giturlparse import parse
from database import *

db = Database('localhost', '5432', 'postgres', 'SuperSekretPassword')
conn = db.connect()
print("Connected")
# cur = conn.cursor()
cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)
cur.execute(
    'SELECT id, scm_url FROM packages WHERE scm_url IS NOT NULL ORDER BY scm_url')

hosts = []
unparseable = []

for record in cur:
    url = record["scm_url"]

    try:
        parsed_url = parse(url)
        host = parsed_url.host
        hosts.append(host)
    except:
        unparseable.append(url)

print(sorted([(host, hosts.count(host)) for host in set(hosts)]))
print('Parsed:', len(hosts))
print('NOT Parsed:', len(unparseable))

cur.execute('SELECT COUNT(*) FROM packages WHERE scm_url IS NULL')
print(cur.fetchall())
