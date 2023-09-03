import os
import sys

from build_packages import BuildPackages
from common.config import Config
from common.packageId import PackageId
from database import Database


def main():
    """
    Standalone script used to recreate a certain build given its build_id (DEBUGGING). Pass in the
    build_id as a command line argument.
    """
    if len(sys.argv) != 2:
        raise ValueError("Please provide only the build_id as a cmd line argument!")

    build_id = sys.argv[1]

    config = Config()
    db = Database(
        config.DB_CONFIG["hostname"],
        config.DB_CONFIG["port"],
        config.DB_CONFIG["username"],
        config.DB_CONFIG["password"],
    )

    os.chdir("./temp/builder")
    builder = BuildPackages(db, config)
    p = db.get_build_params_by_id(build_id)
    if not p:
        raise ValueError("Build does not exist!")
    print(p)

    pkg = PackageId(p["groupid"], p["artifactid"], p["version"])
    path = builder.create_buildspec(
        pkg, p["url"], p["tag_name"], p["tool"], p["jdk"], p["newline"], p["command"]
    )
    result = builder.build(path)
    builder.compare(pkg, build_id, result)
    print(result.stdout)


main()
