import psycopg2
from psycopg2.extras import DictCursor, execute_batch

from packageId import PackageId


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
        self.ERR_TABLE = 'errors'
        self.HOST_URL_FIELDS = {
            'scm_url': ('url','host'),
            'homepage_url': ('url_home','host_home'),
            'dev_conn_url': ('url_dev_conn','host_dev_conn'),
            'scm_conn_url': ('url_scm_conn','host_scm_conn'),
        }

    def get_urls(self, fieldname: str):
        self.cur.execute(
            f'''
            SELECT split_part(id, ':', 1) AS groupid,
            split_part(id, ':', 2) AS artifactid,
            split_part(id, ':', 3) AS version,
            {fieldname}
            FROM {self.PKG_TABLE}
            WHERE {fieldname} IS NOT NULL AND {fieldname} != '';
            '''
        )
        return self.cur.fetchall()
    

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
        self.cur.execute(f'SELECT * FROM {self.HOST_TABLE} ORDER BY url ASC')
        return self.cur.fetchall()


    def insert_hosts(self, groupids: list, artifactids: list, versions: list, urls: list, 
                     hostnames: list, field: str):
        url_field, host_field = self.HOST_URL_FIELDS[field]
        query = (f'''INSERT INTO {self.HOST_TABLE} (groupid,artifactid,version,{url_field},{host_field}) VALUES (%s,%s,%s,%s,%s)
                 ON CONFLICT(groupid, artifactid, version) DO UPDATE SET {url_field} = EXCLUDED.{url_field}, {host_field} = EXCLUDED.{host_field}''')
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
            host VARCHAR,
            valid BOOL DEFAULT false,
            url_home VARCHAR,
            host_home VARCHAR,
            valid_home BOOL DEFAULT false,
            url_scm_conn VARCHAR,
            host_scm_conn VARCHAR,
            valid_scm_conn BOOL DEFAULT false,
            url_dev_conn VARCHAR,
            host_dev_conn VARCHAR,
            valid_dev_conn BOOL DEFAULT false,
            processed BOOL default FALSE,
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
        CREATE TABLE IF NOT EXISTS {self.ERR_TABLE}(
            groupid VARCHAR,
            artifactid VARCHAR,
            version VARCHAR,
            url VARCHAR,
            error VARCHAR
        )
        '''
        self.cur.execute(query)
        self.conn.commit()
    
    def insert_error(self, pkg: PackageId, url: str, err: str):
        query = f'''
        INSERT INTO {self.ERR_TABLE}
        (groupid, artifactid, version, url, error)
        VALUES (%s,%s,%s,%s,%s)
        '''
        self.cur.execute(query, [pkg.groupid, pkg.artifactid, pkg.version, url, err])
        self.conn.commit()
    

    # TODO make this a batch OP
    def update_validity(self, field: str, pkg: PackageId , value: bool):
        query = f'''
        UPDATE {self.HOST_TABLE} SET {field} = {value} 
        WHERE groupid='{pkg.groupid}' AND artifactid='{pkg.artifactid}' AND version='{pkg.version}'
        '''
        self.cur.execute(query)
        self.conn.commit()


    # TODO make this a batch OP
    def mark_processed(self, pkg: PackageId):
        query = f'''
        UPDATE {self.HOST_TABLE} SET processed = true
        WHERE groupid='{pkg.groupid}' AND artifactid='{pkg.artifactid}' AND version='{pkg.version}'
        '''
        self.cur.execute(query)
        self.conn.commit()


    def close(self):
        self.cur.close()
        self.conn.close()
