from builtins import ValueError
import logging
import subprocess
import os
from jinja2 import Template
from psycopg2.extras import DictRow

from database import Database
from packageId import PackageId
from config import Config


class BuildPackages:
    def __init__(self, db: Database, config: Config):
        self.log = logging.getLogger(__name__)
        self.db = db
        self.config = config

    # TODO check returncode and .buildinfo manually when package fails,
    def build_and_compare(self):
        os.chdir("./temp/builder")
        records = self.db.get_hosts_with_tags()
        self.db.create_builds_table()
        total = len(records)
        self.log.info(f"FOUND {total} with tag and outputTimestamp")
        for i, record in enumerate(records):
            self.log.info(f"Processing {i+1}/{total}")
            pkg = PackageId(record["groupid"], record["artifactid"], record["version"])
            buildspecs = self.buildspec_exists(pkg)
            if len(buildspecs) > 0:
                self.build_from_existing(pkg, buildspecs[0])
            else:
                continue
                try:
                    self.build_from_scratch(pkg, record)
                except ValueError as e:
                    # self.log.debug(e)
                    pass

    def build_from_existing(self, pkg: PackageId, buildspec_path):
        self.log.debug(f"{pkg.groupid}:{pkg.artifactid}:{pkg.version}")
        self.log.debug(f"Buildspec found in {buildspec_path}!")
        # TODO build with buildspec and store differently in table
        try:
            build_params = self.parse_buildspec(buildspec_path)
        except ValueError as e:
            self.log.error("Could not parse buildspec!")

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

        with open(
            os.path.join(os.getcwd(), "..", "..", ".buildspec.template"), "r"
        ) as file:
            content = file.read()
        template = Template(content)
        rendered = template.render(values)

        path = (
            f"research/{values['groupId']}-{values['artifactId']}-{values['version']}/"
        )
        if not os.path.exists(path):
            os.makedirs(path)
        filepath = os.path.join(path, ".buildspec")
        with open(filepath, "w") as file:
            file.write(rendered)
        return filepath

    def build_from_scratch(self, pkg: PackageId, record: DictRow):
        urls = [
            record["valid"],
            record["valid_home"],
            record["valid_scm_conn"],
            record["valid_dev_conn"],
        ]
        tags = [record["tag_name"], record["release_tag_name"]]
        # TODO parse the major version
        # jdks = [record["java_version_manifest_3"], record["java_version_manifest_2"]]
        jdks = [self.convert_manifest_3(record["java_version_manifest_3"])]

        notNone = lambda x: x is not None
        urls = list(filter(notNone, urls))
        tags = list(filter(notNone, tags))
        jdks = list(filter(notNone, jdks))

        if len(urls) > 0 and len(tags) > 0 and len(jdks) > 0:
            url = urls[0]
            # TODO maybe try both tags if they are not the same
            tag = tags[0]
            jdk = jdks[0]
        else:
            raise ValueError(
                f"Missing some build params. Only received\nURLS:{urls},\nTags:{tags},\nJDKs:{jdks}"
            )
        for newline in ["lf", "crlf"]:
            buildspec_path = self.create_buildspec(pkg, url, tag, "mvn", jdk, newline)
            self.build(buildspec_path)

    def build(self, buildspec_path):
        process = subprocess.run(
            ["./rebuild.sh", buildspec_path],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        self.log.debug("-------STDOUT-------")
        self.log.debug(process.stdout.decode())
        self.log.debug("-------ENDSTDOUT-------")
        self.log.debug("-------STDERR-------")
        self.log.debug(process.stderr.decode())
        self.log.debug("-------ENDSTDERR-------")
        return process

    def convert_manifest_3(self, version: str):
        mapping = {"1.7": "7", "1.8": "8"}
        if version is None:
            return None
        return mapping.get(
            version, version
        )  # if not found, just return original version

    # TODO if version doesn't exist but other versions exist, use those as templates
    # TODO investigate what happens if git clone with ssh requires fingerprint to continue
    def buildspec_exists(self, pkg: PackageId) -> list:
        # could be in com.github.hazendaz.7zip
        # but also in com.github.hazendaz.7zip.7zip
        # due to incosistency in repo
        paths = []

        base_path = "content/"
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

    def parse_buildspec(self, path):
        # Source the bash file and echo the variables
        command = f'source {path}; echo "$groupId"; echo "$artifactId"; echo "$version"; echo "$tool"; echo "$jdk"; echo "$newline"; echo "$command"'

        # Execute the command in bash
        process = subprocess.Popen(["bash", "-c", command], stdout=subprocess.PIPE)
        output, _ = process.communicate()

        # Decode and split the output into the variable values
        var_values = output.decode().strip().split("\n")

        # Assign the values to Python variables, can throw ValueError
        groupId, artifactId, version, tool, jdk, newline, command = var_values

        self.log.debug(f"groupId = {groupId}")
        self.log.debug(f"artifactId = {artifactId}")
        self.log.debug(f"version = {version}")
        self.log.debug(f"tool = {tool}")
        self.log.debug(f"jdk = {jdk}")
        self.log.debug(f"newline = {newline}")
        self.log.debug(f"command = {command}")
        return {
            "groupid": groupId,
            "artifactid": artifactId,
            "version": version,
            "tool": tool,
            "jdk": jdk,
            "newline": newline,
            "command": command,
        }


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
