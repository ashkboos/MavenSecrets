import psycopg2
from psycopg2.extras import DictCursor, execute_batch


class Database:
    def __init__(self, host, port, user, password):
        self.conn = psycopg2.connect(
            dbname='postgres',
            user=user,
            password=password,
            host=host,
            port=port
        )
        self.cur: DictCursor = self.conn.cursor(cursor_factory=DictCursor)
        self.PKG_TABLE = 'packages'
        self.PKG_LIST_TABLE = 'package_list'
        self.HOST_TABLE = 'hosts'
    

    def get_distinct_urls(self, fieldname: str):
        self.cur.execute(
            f'''
            SELECT DISTINCT ON (split_part(id, ':', 1), split_part(id, ':', 2))
                split_part(id, ':', 1) AS groupid,
                split_part(id, ':', 2) AS artifactid,
                split_part(id, ':', 3) AS version,
                {fieldname}
            FROM {self.PKG_TABLE}
            WHERE {fieldname} IS NOT NULL AND {fieldname} != '';
            '''
        )
        return self.cur.fetchall()

    
    def get_all(self):
        self.cur.execute(f'SELECT * FROM {self.HOST_TABLE} ORDER BY url DESC')
        return self.cur.fetchall()


    def insert_hosts(self, groupids: list, artifactids: list, versions: list, urls: list, hostnames: list):
        query = f"INSERT INTO {self.HOST_TABLE} (groupid,artifactid,version,url,hostname) VALUES (%s,%s,%s,%s,%s)"
        data = list(zip(groupids, artifactids, versions, urls, hostnames))
        execute_batch(self.cur, query, data)
        self.conn.commit()


    def create_table(self):
        query = f'''
        CREATE TABLE IF NOT EXISTS {self.HOST_TABLE}(
            groupid VARCHAR,
            artifactid VARCHAR,
            version VARCHAR,
            url VARCHAR,
            hostname VARCHAR,
            PRIMARY KEY(groupid,artifactid,version)
        )
        '''
        self.cur.execute(query)
        self.conn.commit()
    
    def collate_hosts_yearly(self):
        query = f'''
        SELECT hostname, COUNT(hostname), date_part('year', lastmodified) AS year
        FROM {self.HOST_TABLE} h
        JOIN {self.PKG_LIST_TABLE} pl ON h.groupid = pl.groupid
        AND h.artifactid = pl.artifactid
        AND h.version = pl.version
        GROUP BY year, hostname
        ORDER BY year, hostname;
        '''
        self.cur.execute(query)
        return self.cur.fetchall()

    def create_err_table(self):
        query = f'''
        CREATE TABLE IF NOT EXISTS {self.HOST_TABLE}(
            groupid VARCHAR,
            artifactid VARCHAR,
            version VARCHAR,
            url VARCHAR,
            errors VARCHAR[],
            PRIMARY KEY(groupid,artifactid,version)
        )
        '''
        self.cur.execute(query)
        self.conn.commit()
    

    def close(self):
        self.cur.close()
        self.conn.close()
