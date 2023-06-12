import logging
import psycopg2
from psycopg2.extras import DictCursor, execute_batch

from packageId import PackageId


class Database:
    def __init__(self, host, port, user, password):
        self.log = logging.getLogger(__name__)
        self.conn = psycopg2.connect(
            dbname="postgres", user=user, password=password, host=host, port=port
        )
        self.cur: DictCursor = self.conn.cursor(cursor_factory=DictCursor)

        self.PKG_TABLE = "packages"
        self.PKG_LIST_TABLE = "package_list"
        self.HOST_TABLE = "hosts"
        self.ERR_TABLE = "errors"
        self.TAGS_TABLE = "tags"
        self.HOST_URL_FIELDS = {
            "scm_url": ("url", "host"),
            "homepage_url": ("url_home", "host_home"),
            "dev_conn_url": ("url_dev_conn", "host_dev_conn"),
            "scm_conn_url": ("url_scm_conn", "host_scm_conn"),
        }

    def get_urls(self, fieldname: str):
        self.execute(
            f"""
            SELECT groupid, artifactid, version, {fieldname}
            FROM {self.PKG_TABLE}
            WHERE {fieldname} IS NOT NULL AND {fieldname} != '';
            """
        )
        return self.cur.fetchall()

    def get_valid_github_urls(self):
        """
        Gets all packages that have a valid github url and are not already in tags table.
        This does not exclude packages that have failed before.
        """
        self.execute(
            f"""
            SELECT groupid, artifactid, version, valid, valid_home, valid_scm_conn, valid_dev_conn
            FROM {self.HOST_TABLE} AS h
            WHERE
                ((valid IS NOT NULL
                    AND valid LIKE '%github.com%')
                    OR
                 (valid_home IS NOT NULL
                     AND valid_home LIKE '%github.com%')
                    OR
                 (valid_scm_conn IS NOT NULL
                     AND  valid_scm_conn LIKE '%github.com%')
                    OR
                 (valid_dev_conn IS NOT NULL
                     AND valid_dev_conn LIKE '%github.com%'))
              AND NOT EXISTS
                (SELECT 1
                 FROM {self.TAGS_TABLE} AS t
                 WHERE  h.groupid = t.groupid
                   AND h.artifactid = t.artifactid
                   AND h.version = t.version);
            """
        )
        return self.cur.fetchall()

    def get_distinct_urls(self, fieldname: str):
        self.execute(
            f"""
            SELECT DISTINCT ON (groupid, artifactid)
                groupid, artifactid, version, {fieldname}
            FROM {self.PKG_TABLE}
            WHERE {fieldname} IS NOT NULL AND {fieldname} != '';
            """
        )
        return self.cur.fetchall()

    def get_all(self):
        self.execute(f"SELECT * FROM {self.HOST_TABLE} ORDER BY url ASC")
        return self.cur.fetchall()

    def get_all_unprocessed(self):
        self.execute(
            f"SELECT * FROM {self.HOST_TABLE} WHERE processed = false ORDER BY url ASC"
        )
        return self.cur.fetchall()

    def insert_hosts(
        self,
        groupids: list,
        artifactids: list,
        versions: list,
        urls: list,
        hostnames: list,
        field: str,
    ):
        url_field, host_field = self.HOST_URL_FIELDS[field]
        query = f"""INSERT INTO {self.HOST_TABLE} (groupid,artifactid,version,{url_field},{host_field}) VALUES (%s,%s,%s,%s,%s)
                 ON CONFLICT(groupid, artifactid, version) DO UPDATE SET {url_field} = EXCLUDED.{url_field}, {host_field} = EXCLUDED.{host_field}"""
        data = list(zip(groupids, artifactids, versions, urls, hostnames))
        execute_batch(self.cur, query, data)
        self.conn.commit()

    def create_table(self):
        query = f"""
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
        """
        self.execute(query)
        self.conn.commit()

    def collate_hosts_yearly(self, field: str):
        mapping = {
            "host": "valid",
            "host_home": "valid_home",
            "host_scm_conn": "valid_scm_conn",
            "host_dev_conn": "valid_dev_conn",
        }

        query = f"""
        SELECT {field}, COUNT({field}) AS count, date_part('year', lastmodified) AS year
        FROM {self.HOST_TABLE} h
        JOIN {self.PKG_LIST_TABLE} pl ON h.groupid = pl.groupid
        AND h.artifactid = pl.artifactid
        AND h.version = pl.version
        WHERE h.{mapping[field]} IS NOT NULL
        GROUP BY year, {field}
        ORDER BY year, count DESC;
        """
        self.execute(query)
        return self.cur.fetchall()

    def create_tags_table(self):
        self.execute(
            f"""
        CREATE TABLE IF NOT EXISTS {self.TAGS_TABLE}(
            groupid      TEXT NOT NULL,
            artifactid   TEXT NOT NULL,
            version      TEXT NOT NULL,
            tag_name     TEXT,
            tag_commit_hash  TEXT,
            release_name TEXT,
            release_tag_name TEXT,
            release_commit_hash TEXT,
            url TEXT,
            PRIMARY KEY (artifactid, groupid, version)
        )
        """
        )
        self.conn.commit()

    # TODO change on conflict to just update the values!
    def insert_tag(
        self,
        pkg: PackageId,
        url: str,
        tag_name=None,
        tag_commit_hash=None,
        release_name=None,
        release_tag_name=None,
        release_commit_hash=None,
    ):
        query = f"""
        INSERT INTO tags(groupid, artifactid, version, tag_name, tag_commit_hash, release_name, release_tag_name, release_commit_hash, url)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)
        ON CONFLICT DO NOTHING;
        """
        self.execute(
            query,
            [
                pkg.groupid,
                pkg.artifactid,
                pkg.version,
                tag_name,
                tag_commit_hash,
                release_name,
                release_tag_name,
                release_commit_hash,
                url,
            ],
        )
        self.conn.commit()

    def create_err_table(self):
        query = f"""
        CREATE TABLE IF NOT EXISTS {self.ERR_TABLE}(
            groupid VARCHAR,
            artifactid VARCHAR,
            version VARCHAR,
            url VARCHAR,
            error VARCHAR
        )
        """
        self.execute(query)
        self.conn.commit()

    def insert_error(self, pkg: PackageId, url: str, err: str):
        query = f"""
        INSERT INTO {self.ERR_TABLE}
        (groupid, artifactid, version, url, error)
        VALUES (%s,%s,%s,%s,%s)
        """
        self.execute(query, [pkg.groupid, pkg.artifactid, pkg.version, url, err])
        self.conn.commit()

    # TODO make this a batch OP
    def update_validity(self, field: str, pkg: PackageId, url: str):
        query = f"""
        UPDATE {self.HOST_TABLE} SET {field} = '{url}'
        WHERE groupid='{pkg.groupid}' AND artifactid='{pkg.artifactid}' AND version='{pkg.version}'
        """
        self.execute(query)
        self.conn.commit()

    # TODO make this a batch OP
    def mark_processed(self, pkg: PackageId):
        query = f"""
        UPDATE {self.HOST_TABLE} SET processed = true
        WHERE groupid='{pkg.groupid}' AND artifactid='{pkg.artifactid}' AND version='{pkg.version}'
        """
        self.execute(query)
        self.conn.commit()

    def get_hosts_with_tags(self):
        query = f"""
        SELECT t.groupid, t.artifactid, t.version, tag_name, release_tag_name,
               valid, valid_home, valid_dev_conn, valid_scm_conn, java_version_manifest_2,
               java_version_manifest_3, java_version_class_major, output_timestamp_prop
        FROM tags AS t
        JOIN hosts h on t.groupid = h.groupid
            AND t.artifactid = h.artifactid
            AND t.version = h.version
        JOIN packages p on t.groupid = p.groupid
            AND t.artifactid = p.artifactid
            AND t.version = p.version
        ORDER BY t.version
        """
        # TODO remove the ORDER BY!!!!
        self.execute(query)
        return self.cur.fetchall()

    def execute(self, query: str, vars: list = None):
        if vars is None:
            self.log.debug(f"Executing query: {query}")
        else:
            self.log.debug(f"Executing query: {query} with values {vars}")
        self.cur.execute(query, vars)

    def close(self):
        self.cur.close()
        self.conn.close()


# SELECT *
# FROM errors AS e
# WHERE NOT EXISTS (
#     SELECT 1
#     FROM hosts AS h
#     WHERE e.groupid = h.groupid
#     AND e.artifactid = h.artifactid
#     AND e.version = h.version
# );

# Query to find all packages that didn't have urls or the hostnames couldn't be parsed
# SELECT *
# FROM packages AS p
# WHERE NOT EXISTS (
#     SELECT 1
#     FROM hosts AS h
#     WHERE p.groupid = h.groupid
#     AND p.artifactid = h.artifactid
#     AND p.version = h.version
# );