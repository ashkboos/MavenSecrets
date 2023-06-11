import logging
import subprocess
import os
from jinja2 import Template
from psycopg2.extras import DictRow

from database import Database
from packageId import PackageId


class BuildPackages:
    def __init__(self, db: Database):
        self.log = logging.getLogger(__name__)
        self.db = db

    # TODO check returncode and .buildinfo manually when package fails,
    def build_and_compare(self):
        records = self.db.get_hosts_with_tags()
        for record in records:
            pkg = PackageId(record["groupid"], record["artifactid"], record["version"])
            buildspecs = self.buildspec_exists(pkg)
            if len(buildspecs) > 0:
                self.log.debug(f"{pkg.groupid}:{pkg.artifactid}:{pkg.version}")
                self.log.debug(f"Buildspec found in {buildspecs}!")
                buildspec = buildspecs[0]
                self.log.debug(f"Return code: {process.returncode}")
                # self.log.debug(process.stdout.decode())
                # self.log.critical(process.stderr.decode())
            else:
                self.build_from_scratch(pkg, record)

    def clone_rep_central(self):
        clone_dir = "./temp/builder"
        url = "https://github.com/jvm-repo-rebuild/reproducible-central.git"
        process = subprocess.run(["git", "clone", url, clone_dir])
        if process.returncode != 0:
            self.log.error("Problem encountered")

    def create_buildspec(
        self, pkg: PackageId, git_repo, git_tag, tool, jdk, newline
    ) -> str:
        values = {
            "groupId": pkg.groupid,
            "artifactId": pkg.artifactid,
            "version": pkg.version,
            "gitRepo": git_repo,
            "gitTag": git_tag,
            "tool": tool,
            "jdk": jdk,
            "newline": newline,
        }

        with open(".buildspec.template", "r") as file:
            content = file.read()
        template = Template(content)
        rendered = template.render(values)

        path = f"./temp/builder/research/{values['groupId']}-{values['artifactId']}-{values['version']}/"
        if not os.path.exists(path):
            os.makedirs(path)
        filepath = os.path.join(path, ".buildspec")
        with open(filepath, "w") as file:
            file.write(rendered)
        return filepath

    def build_from_scratch(self, pkg: PackageId, record: DictRow) -> bool:
        urls = [
            record["valid"],
            record["valid_home"],
            record["valid_scm_conn"],
            record["valid_dev_conn"],
        ]
        tags = [record["tag_name"], record["release_tag_name"]]
        jdks = [record["java_version_manifest_3"], record["java_version_manifest_2"]]
        urls = list(filter(lambda x: x is not None, urls))
        tags = list(filter(lambda x: x is not None, tags))
        jdks = list(filter(lambda x: x is not None, jdks))
        # TODO parse the major version

        if len(urls) > 0 and len(tags) > 0 and len(jdks):
            url = urls[0]
            # TODO maybe try both tags if they are not the same
            tag = tags[0]
            jdk = jdks[0]
        else:
            return False

        buildspec_path = self.create_buildspec(pkg, url, tag, "mvn", jdk, "lf")
        self.build(buildspec_path)

    def build(self, buildspec_path):
        return subprocess.run(["./temp/builder/rebuild.sh", buildspec_path])

    # TODO if version doesn't exist but other versions exist, use those as templates
    # TODO investigate what happens if git clone with ssh requires fingerprint to continue
    def buildspec_exists(self, pkg: PackageId) -> list:
        # could be in com.github.hazendaz.7zip
        # but also in com.github.hazendaz.7zip.7zip
        # due to incosistency in repo
        paths = []

        base_path = "./temp/builder/content/"
        # path with artifactid
        relative_path = (
            pkg.groupid.replace(".", "/")
            + "/"
            + pkg.artifactid
            + "/"
            + pkg.artifactid
            + "-"
            + pkg.version
            + ".buildspec"
        )
        path = os.path.join(base_path, relative_path)
        if pkg.artifactid == "7zip":
            self.log.debug(path)
        if os.path.exists(path):
            paths.append(path)

        # path without artifactid
        relative_path = (
            pkg.groupid.replace(".", "/")
            + "/"
            + pkg.artifactid
            + "-"
            + pkg.version
            + ".buildspec"
        )
        path = os.path.join(base_path, relative_path)
        if pkg.artifactid == "7zip":
            self.log.debug(path)
        if os.path.exists(path):
            paths.append(path)

        return paths

    def compare(self):
        pass


# Examples

# values = {
#     "groupId": "com.github.hazendaz.7zip",
#     "artifactId": "7zip",
#     "version": "23.00",
#     "gitRepo": "https://github.com/hazendaz/7-zip.git",
#     "gitTag": "7zip-23.00",
#     "tool": "mvn-3.9.2",
#     "jdk": "17",
#     "newline": "crlf",
# }

# values = {
#     "groupId": "io.cucumber",
#     "artifactId": "gherkin",
#     "version": "26.2.0",
#     "gitRepo": "https://github.com/cucumber/gherkin.git",
#     "gitTag": "v26.2.0",
#     "tool": "mvn-3.9.2",
#     "jdk": "11",
#     "newline": "lf",
# }

# WARNING] The requested profile "apache-release" could not be activated because it does not exist.
# dos2unix: target/matsuo-util-common-0.1.3.buildinfo: No such file or directory
# dos2unix: Skipping target/matsuo-util-common-0.1.3.buildinfo, not a regular file.
