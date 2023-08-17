from builtins import ValueError
import pandas as pd
import glob
from itertools import takewhile, dropwhile
import logging
import re
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
        self.db.create_builds_table()
        records = self.db.get_hosts_with_tags()
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
            # remove folder once all builds for the package are complete
            folder = f"research/{pkg.groupid}-{pkg.artifactid}-{pkg.version}/"
            if os.path.isdir(folder):
                shutil.rmtree(folder)

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

    def build_from_scratch(self, pkg: PackageId, record: DictRow):
        url = record["url"]
        tags = [record["tag_name"], record["release_tag_name"]]
        build_jdk_spec = self.convert_jdk_version(record["java_version_manifest_3"])
        build_jdk = self.convert_jdk_version(
            self.parse_build_jdk(record["java_version_manifest_2"])
        )
        source_jdk_ver = record["compiler_version_source"]
        pub_date = record["lastmodified"]

        jdks = []
        if build_jdk_spec:
            jdks.append(build_jdk_spec)
        elif build_jdk:
            jdks.append(build_jdk)
        else:
            # build with every LTS version available at package release
            jdks.extend(self.choose_jdk_versions(source_jdk_ver, pub_date, lts_only=True))

        notNone = lambda x: x is not None
        tags = list(filter(notNone, tags))

        if len(tags) > 0:
            tag = tags[0]
        else:
            raise ValueError(f"Missing some build params.")
        self.log.debug(jdks)
        for jdk in jdks:
            for newline in ["lf", "crlf"]:
                buildspec_path = self.create_buildspec(
                    pkg, url, tag, "mvn", jdk, newline
                )
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
        paths = []

        base_path = "content/"
        # buildspec could be in com.github.hazendaz.7zip but also in
        # com.github.hazendaz.7zip.7zip due to incosistency in repo path with artifactid
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
        """Parses the buildspec by sourcing the bash variables from the buildspec
        Throws: ValueError
        Returns: Build_Spec object
        """
        # Source the bash file and echo the variables
        command = f'source {path}; echo "$groupId"; echo "$artifactId"; echo "$version"; echo "$tool"; echo "$jdk"; echo "$newline"; echo "$command"'

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

    def choose_jdk_versions(
        self, jdk_src_ver: str, pub_date: str, lts_only: bool
    ) -> list:
        """Given the source jdk version and the package's publish date, returns
        all jdk versions available at that date. Only LTS versions are returned if 
        lts_only = true.
        """
        pub_date = pd.to_datetime(pub_date)
        data = {
            "0": "1996-01-23",
            "1": "1997-02-02",
            "2": "1998-12-04",
            "3": "2000-05-08",
            "4": "2002-02-13",
            "5": "2004-09-29",
            "6": "2006-12-11",
            "7": "2011-07-28",
            "8": "2014-03-18",
            "9": "2017-09-21",
            "10": "2018-03-20",
            "11": "2018-09-25",
            "12": "2019-03-19",
            "13": "2019-09-17",
            "14": "2020-03-17",
            "15": "2020-09-16",
            "16": "2021-03-16",
            "17": "2021-09-14",
            "18": "2022-03-22",
            "19": "2022-09-20",
            "20": "2023-03-21",
            "21": "2023-09-19",
        }
        jdk_rel_dates = {k: pd.to_datetime(v) for k, v in data.items()}

        all_vers_after = dict(
            dropwhile(lambda kv: kv[0] != jdk_src_ver, jdk_rel_dates.items())
        )
        vers_at_publish = dict(
            takewhile(lambda kv: kv[1] < pub_date, all_vers_after.items())
        )
        if lts_only:
            return [
                ver
                for ver in vers_at_publish
                if ver in [jdk_src_ver, "8", "11", "17", "21"]
            ]
        else:
            return [ver for ver in vers_at_publish]

    def convert_jdk_version(self, version: str):
        """Converts 1.X style JDK version to the X version
        i.e. 1.8 returns 8
        """
        if version is None:
            return None
        else:
            return re.sub(r"1\.([0-9]|1[0-9]|20|21)", r"\1", version)

    def parse_build_jdk(self, version) -> str:
        """Parses the major JDK version from the highly specific format
        returned by the java.version system property.
        """
        if not version:
            return None

        result = re.search(
            r"(?:(1\.\d)|[2-9](?=\.\d)|(\d{2}|\d{1}(?![\d\.])))", version
        )
        if result is None:
            self.log.debug(f"COULDN'T PARSE {version}")
            return None

        major_ver = result.group()
        try:
            if float(major_ver) > 21:
                self.log.debug(f"WRONG PARSING OF {version}")
                return None
        except ValueError:
            return None
        return major_ver

    def clone_rep_central(self):
        clone_dir = "./temp/builder"
        url = "https://github.com/Vel1khan/reproducible-central.git"
        process = subprocess.run(["git", "clone", url, clone_dir])
        if process.returncode != 0:
            self.log.error("Problem encountered")
