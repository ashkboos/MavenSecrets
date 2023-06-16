from builtins import ValueError
import glob
import logging
import subprocess
import os
import shutil
from jinja2 import Template
from psycopg2.extras import DictRow

from database import Database
from common.packageId import PackageId
from common.config import Config
from common.build_result import Build_Result
from common.build_spec import Build_Spec


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
                self.log.debug(f"Buildspec found in {buildspecs[0]}!")
                self.build_from_existing(pkg, buildspecs[0])
            else:
                try:
                    self.build_from_scratch(pkg, record)
                except ValueError as e:
                    self.log.debug(e)
                    continue

    def build_from_existing(self, pkg: PackageId, src_buildspec):
        # Copy buildspec to research/ folder
        dest = f"research/{pkg.groupid}-{pkg.artifactid}-{pkg.version}/"
        if not os.path.exists(dest):
            os.makedirs(dest)
        buildspec_path = os.path.join(dest, f"{pkg.artifactid}-{pkg.version}.buildspec")
        shutil.copyfile(src_buildspec, buildspec_path)

        try:
            build_spec = self.parse_buildspec(buildspec_path)
        except ValueError:
            self.log.error("Could not parse buildspec!")
        build_result = self.build(buildspec_path, pkg)
        self.db.insert_build(build_spec, build_result, from_existing=True)

    # TODO deal with changing params
    def build_from_scratch(self, pkg: PackageId, record: DictRow):
        url = record["url"]
        tags = [record["tag_name"], record["release_tag_name"]]
        # TODO parse the major version from manifest_2 field
        # jdks = [record["java_version_manifest_3"], record["java_version_manifest_2"]]
        jdks = [self.convert_manifest_3(record["java_version_manifest_3"])]

        notNone = lambda x: x is not None
        tags = list(filter(notNone, tags))
        jdks = list(filter(notNone, jdks))

        if len(tags) > 0 and len(jdks) > 0:
            # TODO maybe try both tags if they are not the same
            tag = tags[0]
            jdk = jdks[0]
        else:
            raise ValueError(f"Missing some build params.")
        for newline in ["lf", "crlf"]:
            buildspec_path = self.create_buildspec(pkg, url, tag, "mvn", jdk, newline)
            buildspec = self.parse_buildspec(buildspec_path)
            build_result = self.build(buildspec_path, pkg)
            self.db.insert_build(buildspec, build_result, False)

    def build(self, buildspec_path, pkg: PackageId):
        # process = subprocess.run(["./rebuild.sh", buildspec_path])
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

        dir_path = os.path.dirname(buildspec_path)
        search_pattern = os.path.join(dir_path, "*.buildcompare")
        files = glob.glob(search_pattern)
        if len(files) == 0:
            return Build_Result(
                False, process.stdout.decode(), process.stderr.decode(), None, None
            )
        self.log.debug(f"{len(files)} .buildcompare files found:\n{files}")
        try:
            with open(files[0], "r") as file:
                result = {}
                for line in file:
                    try:
                        key, value = line.strip().split("=", 1)
                    except ValueError:
                        continue
                    if key in ["ok", "ko"]:
                        continue
                    if key in ["okFiles", "koFiles"]:
                        result[key] = value.strip('"').split(" ")
                    else:
                        result[key] = value.strip('"')
                okFiles, koFiles = result.get("okFiles"), result.get("koFiles")
            return Build_Result(
                True, process.stdout.decode(), process.stderr.decode(), okFiles, koFiles
            )

        except (FileNotFoundError, KeyError):
            self.log.debug("File not found or malformed. Build (probably) failed")
            return Build_Result(
                False, process.stdout.decode(), process.stderr.decode(), None, None
            )

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

        path = f"research/{pkg.groupid}-{pkg.artifactid}-{pkg.version}/"
        if not os.path.exists(path):
            os.makedirs(path)
        filepath = os.path.join(path, f"{pkg.artifactid}-{pkg.version}.buildspec")
        with open(filepath, "w") as file:
            file.write(rendered)
        return filepath

    def parse_buildspec(self, path) -> Build_Spec:
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
        return Build_Spec(groupId, artifactId, version, tool, jdk, newline, command)

    def compare(self):
        pass

    def clone_rep_central(self):
        clone_dir = "./temp/builder"
        url = "https://github.com/jvm-repo-rebuild/reproducible-central.git"
        process = subprocess.run(["git", "clone", url, clone_dir])
        if process.returncode != 0:
            self.log.error("Problem encountered")

    def convert_manifest_3(self, version: str):
        mapping = {"1.7": "7", "1.8": "8"}
        if version is None:
            return None
        return mapping.get(
            version, version
        )  # if not found, just return original version


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
