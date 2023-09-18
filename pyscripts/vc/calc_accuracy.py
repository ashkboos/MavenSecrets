import os

from build_packages import BuildPackages
from common.config import Config
from common.packageId import PackageId
from database import Database
import logging
import subprocess

from utils import get_field


class CalcAccuracy:

    def __init__(self, db: Database, config: Config, bp: BuildPackages):
        self.log = logging.getLogger(__name__)
        self.db = db
        self.config = config
        self.bp = bp


    def calc_accuracy(self):
        records = self.db.get_packages_with_rc()
        total = len(records)
        self.log.info(f"FOUND {total} with buildspec")
        url_counter = tag_counter = jdk_counter = newline_counter = 0

        for i, record in enumerate(records):
            pkg = PackageId(record["groupid"], record["artifactid"], record["version"])
            buildspec_path = self.db.get_buildspec_path(pkg)
            if not os.path.exists(buildspec_path):
                total -=1
                continue
            buildspec = self.bp.parse_buildspec(buildspec_path)

            jdks, newlines, tag, url = self.get_fields(record)

            if(are_repos_equal(buildspec.gitRepo, url)):
                url_counter+=1

            if(buildspec.gitTag == tag):
                tag_counter+=1

            if(buildspec.jdk.split('.')[0] in jdks):
                jdk_counter+=1

            if(buildspec.newline in newlines):
                newline_counter+=1


        print("url: ", url_counter/total,
              "tag: ", tag_counter/total,
              "jdk: ", jdk_counter/total,
              "newline: ", newline_counter/total)

    def get_fields(self, record):
        url, tag = get_field(record, "url", mandatory=True), get_field(record, "tag_name", mandatory=True)
        pub_date = get_field(record, "lastmodified", mandatory=True)
        nline_inconsistent = get_field(record, "line_ending_inconsistent_in_file")
        nline_lf, nline_crlf = get_field(record, "line_ending_lf"), get_field(record, "line_ending_crlf")

        build_jdk_spec = self.bp.convert_jdk_version(get_field(record, "java_version_manifest_3"))
        build_jdk = self.bp.convert_jdk_version(self.bp.parse_build_jdk(get_field(record, "java_version_manifest_2")))
        source_jdk_ver = self.bp.convert_jdk_version(get_field(record, "compiler_version_source"))

        jdks = []
        if build_jdk_spec:
            jdks.append(build_jdk_spec)
        elif build_jdk:
            jdks.append(build_jdk)
        else:
            jdks.extend(self.bp.choose_jdk_versions(source_jdk_ver, pub_date, lts_only=True))

        if nline_lf and not nline_crlf and not nline_inconsistent:
            newlines = ["lf"]
            self.log.debug("Newlines detected from pom.properties. Building with lf.")
        elif not nline_lf and nline_crlf and not nline_inconsistent:
            newlines = ["crlf"]
            self.log.debug("Newlines detected from pom.properties. Building with crlf.")
        else:
            self.log.debug("Newlines NOT detected. Building with lf & crlf.")
            newlines = ["lf", "crlf"]

        return jdks, newlines, tag, url


def get_head_commit_hash(repo_url):
    """Fetch the latest commit hash of the default branch for a repository URL."""
    try:
        # Get the commit hash of the default branch (HEAD) for the given repository URL
        commit_hash = subprocess.check_output(['git', 'ls-remote', repo_url, 'HEAD']).split()[0].decode('utf-8')
        return commit_hash
    except subprocess.CalledProcessError:
        return None

def are_repos_equal(url1, url2):
    """Determine if two repository URLs point to repositories with the same latest commit."""
    commit_hash_1 = get_head_commit_hash(url1)
    commit_hash_2 = get_head_commit_hash(url2)

    if not commit_hash_1 or not commit_hash_2:
        return None

    return commit_hash_1 == commit_hash_2


if __name__ == '__main__':
    config = Config()
    db = Database(
        config.DB_CONFIG["hostname"],
        config.DB_CONFIG["port"],
        config.DB_CONFIG["username"],
        config.DB_CONFIG["password"],
    )
    bp = BuildPackages(db, config)
    ca = CalcAccuracy(db, config, bp)
    ca.calc_accuracy()


