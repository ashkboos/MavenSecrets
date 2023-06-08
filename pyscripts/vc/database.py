import logging
import psycopg2
from psycopg2.extras import DictCursor, execute_batch

from packageId import PackageId


class Database:
    def __init__(self, host, port, user, password):
        self.log = logging.getLogger(__name__)
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
        self.execute(
            f'''
            SELECT groupid, artifactid, version, {fieldname}
            FROM {self.PKG_TABLE}
            WHERE {fieldname} IS NOT NULL AND {fieldname} != '';
            '''
        )
        return self.cur.fetchall()
    

    def get_distinct_urls(self, fieldname: str):
        self.execute(
            f'''
            SELECT DISTINCT ON (groupid, artifactid)
                groupid, artifactid, version, {fieldname}
            FROM {self.PKG_TABLE}
            WHERE {fieldname} IS NOT NULL AND {fieldname} != '';
            '''
        )
        return self.cur.fetchall()

    
    def get_all(self):
        self.execute(f'SELECT * FROM {self.HOST_TABLE} ORDER BY url ASC')
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
            valid VARCHAR,
            url_home VARCHAR,
            host_home VARCHAR,
            valid_home VARCHAR,
            url_scm_conn VARCHAR,
            host_scm_conn VARCHAR,
            valid_scm_conn VARCHAR,
            url_dev_conn VARCHAR,
            host_dev_conn VARCHAR,
            valid_dev_conn VARCHAR,
            processed BOOL default FALSE,
            PRIMARY KEY(groupid,artifactid,version)
        )
        '''
        self.execute(query)
        self.conn.commit()
    

    def collate_hosts_yearly(self, field: str):
        mapping = {'host': 'valid', 'host_home': 'valid_home', 'host_scm_conn': 'valid_scm_conn', 'host_dev_conn': 'valid_dev_conn'}
        
        query = f'''
        SELECT {field}, COUNT({field}) AS count, date_part('year', lastmodified) AS year
        FROM {self.HOST_TABLE} h
        JOIN {self.PKG_LIST_TABLE} pl ON h.groupid = pl.groupid
        AND h.artifactid = pl.artifactid
        AND h.version = pl.version
        WHERE h.{mapping[field]} IS NOT NULL
        GROUP BY year, {field}
        ORDER BY year, count DESC;
        '''
        self.execute(query)
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
        self.execute(query)
        self.conn.commit()
    
    def insert_error(self, pkg: PackageId, url: str, err: str):
        query = f'''
        INSERT INTO {self.ERR_TABLE}
        (groupid, artifactid, version, url, error)
        VALUES (%s,%s,%s,%s,%s)
        '''
        self.execute(query, [pkg.groupid, pkg.artifactid, pkg.version, url, err])
        self.conn.commit()
    

    # TODO make this a batch OP
    def update_validity(self, field: str, pkg: PackageId , url: str):
        query = f'''
        UPDATE {self.HOST_TABLE} SET {field} = '{url}'
        WHERE groupid='{pkg.groupid}' AND artifactid='{pkg.artifactid}' AND version='{pkg.version}'
        '''
        self.execute(query)
        self.conn.commit()


    # TODO make this a batch OP
    def mark_processed(self, pkg: PackageId):
        query = f'''
        UPDATE {self.HOST_TABLE} SET processed = true
        WHERE groupid='{pkg.groupid}' AND artifactid='{pkg.artifactid}' AND version='{pkg.version}'
        '''
        self.execute(query)
        self.conn.commit()
    
    def execute(self, query: str):
        self.log.debug(f'Executing query: {query}')
        self.cur.execute(query)


    def close(self):
        self.cur.close()
        self.conn.close()
