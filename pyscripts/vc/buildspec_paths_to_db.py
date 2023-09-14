import psycopg2
from build_packages import BuildPackages
from common.config import Config
from common.packageId import PackageId
from psycopg2.extras import DictCursor
from rc_path_finder import create_build_spec_coord2path_dic


class Main:
    """
    Standalone script that searches for the buildspec of every package in RC
    and updates it in the packages table in the row of its respective package.
    """

    def __init__(self, config: Config) -> None:
        host, port = config.DB_CONFIG["hostname"], config.DB_CONFIG["port"]
        user, password = config.DB_CONFIG["username"], config.DB_CONFIG["password"]
        self.conn = psycopg2.connect(
            dbname="postgres", user=user, password=password, host=host, port=port
        )
        self.cur: DictCursor = self.conn.cursor(cursor_factory=DictCursor)

        self.PKG_TABLE = "packages"

    def run(self):
        dic: dict[str, str] = create_build_spec_coord2path_dic("temp/builder/")
        pkg_to_path = {
            PackageId(part[0], part[1], part[2]): path
            for (coord, path) in dic.items()
            for part in [coord.split(":")]
        }
        print(pkg_to_path)
        self.add_col_if_not_exists()
        self.insert_or_update_path(pkg_to_path)

    def insert_or_update_path(self, pkg_to_path: dict[PackageId, str]):
        query = """
        UPDATE packages
        SET buildspec_path = %s
        WHERE groupid = %s AND artifactid = %s AND version = %s;
        """
        self.cur.executemany(
            query, [(v, k.groupid, k.artifactid, k.version) for k, v in pkg_to_path.items()]
        )
        self.conn.commit()

    def add_col_if_not_exists(self):
        query = """
        ALTER TABLE packages ADD COLUMN IF NOT EXISTS buildspec_path TEXT;
        """
        self.cur.execute(query)
        self.conn.commit()


if __name__ == "__main__":
    config = Config()
    Main(config).run()
