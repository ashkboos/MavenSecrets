import glob
import logging
import os
import re
import shutil
import subprocess
from builtins import ValueError
from itertools import dropwhile, takewhile

import pandas as pd
from common.build_result import Build_Result
from common.build_spec import Build_Spec
from common.config import Config
from common.packageId import PackageId
from database import Database
from jinja2 import Template
from psycopg2.extras import DictRow
from utils import compare_jars, extract_path_buildinfo, get_field


class BuildPackages:
    def __init__(self, db: Database, config: Config):
        self.log = logging.getLogger(__name__)
        self.db = db
        self.config = config
        self.db.create_builds_table()
        self.db.create_jar_repr_table()
        self.db.create_err_table()

    # TODO check returncode and .buildinfo manually when package fails,
    def build_all(self):
        """Fetches all packages that have not been built yet and have the
        correct build parameter data, then builds them one by one, manually
        generating a buildspec. If the buildspec already exists on Reproducible
        Central, also build using that.
        """
        os.chdir("./temp/builder")
        if self.config.BUILD_LIST:
            self.log.info(f"{len(self.config.BUILD_LIST)} packages in build list")
            records = self.db.get_pkgs_from_list_with_tags(self.config.BUILD_LIST)
            missing = [
                pkg
                for pkg in self.config.BUILD_LIST
                if pkg
                not in [
                    PackageId(row["groupid"], row["artifactid"], row["version"]) for row in records
                ]
            ]
            self.log.error(f"Missing packages: {missing}")
            self.log.info(f"FOUND {len(records)} packages to build.")
            if len(records) != len(self.config.BUILD_LIST):
                raise ValueError(
                    "Not all packages from build list were found and/or did "
                    + "not have all necessary build params in DB."
                )
        else:
            records = self.db.get_pkgs_with_tags()
            self.log.info(f"FOUND {len(records)} packages to build.")

        for i, record in enumerate(records):
            self.log.info(f"Processing {i+1}/{len(records)}")
            pkg = PackageId(record["groupid"], record["artifactid"], record["version"])
            buildspecs = self.buildspec_exists(pkg)
            if len(buildspecs) > 0:
                self.log.debug(f"Buildspec found in {buildspecs[0]}!")
                self.build_from_existing(pkg, buildspecs[0])
            try:
                self.build_from_scratch(pkg, record)
            except ValueError as err:
                self.log.debug(err)

            # remove folder once all builds for the package are complete
            # folder = f"research/{pkg.groupid}-{pkg.artifactid}-{pkg.version}/"
            # if os.path.isdir(folder):
            #     shutil.rmtree(folder)

    def build_from_existing(self, pkg: PackageId, src_buildspec):
        """Given a package and the path to its pre-existing buildspec from Reproducible
        Central, builds it and saves the build result along with the parsed build params
        to the db.
        """
        dest = f"research/{pkg.groupid}-{pkg.artifactid}-{pkg.version}/"
        if not os.path.exists(dest):
            os.makedirs(dest)
        buildspec_path = os.path.join(dest, f"{pkg.artifactid}-{pkg.version}.buildspec")
        shutil.copyfile(src_buildspec, buildspec_path)

        try:
            build_spec = self.parse_buildspec(buildspec_path)
        except ValueError:
            self.db.insert_error(
                pkg,
                None,
                f"(BUILDER) Could not parse buildspec with path {buildspec_path}",
            )
            return
        build_result = self.build(buildspec_path)
        build_id = self.db.insert_build(build_spec, build_result, from_existing=True)
        self.compare(pkg, build_id, build_result)

    def build_from_scratch(self, pkg: PackageId, record: DictRow):
        """Given a package and its associated data, generates (multiple)
        buildspecs and builds them, inserting the Build_Result into
        the database.

        Throws:
        ValueError if mandatory fields are missing,
        KeyError if record does not contain requested fields.
        """
        url, tag = get_field(record, "url", mandatory=True), get_field(
            record, "tag_name", mandatory=True
        )
        pub_date = get_field(record, "lastmodified", mandatory=True)
        nline_inconsistent = get_field(record, "line_ending_inconsistent_in_file")
        nline_lf, nline_crlf = get_field(record, "line_ending_lf"), get_field(
            record, "line_ending_crlf"
        )
        build_jdk_spec = self.convert_jdk_version(get_field(record, "java_version_manifest_3"))
        build_jdk = self.convert_jdk_version(
            self.parse_build_jdk(get_field(record, "java_version_manifest_2"))
        )
        source_jdk_ver = self.convert_jdk_version(get_field(record, "compiler_version_source"))

        jdks = []
        if build_jdk_spec:
            jdks.append(build_jdk_spec)
        elif build_jdk:
            jdks.append(build_jdk)
        else:
            # build with every LTS version available at package release
            jdks.extend(self.choose_jdk_versions(source_jdk_ver, pub_date, lts_only=True))
            self.log.info(f"No compiler JDK version found. Building with versions: {jdks}")

        if nline_lf and not nline_crlf and not nline_inconsistent:
            newlines = ["lf"]
            self.log.debug("Newlines detected from pom.properties. Building with lf.")
        elif not nline_lf and nline_crlf and not nline_inconsistent:
            newlines = ["crlf"]
            self.log.debug("Newlines detected from pom.properties. Building with crlf.")
        else:
            self.log.debug("Newlines NOT detected. Building with lf & crlf.")
            newlines = ["lf", "crlf"]

        for jdk in jdks:
            for newline in newlines:
                buildspec_path = self.create_buildspec(
                    pkg, url, tag, "mvn", jdk, newline, self.config.BUILD_CMD
                )
                buildspec = self.parse_buildspec(buildspec_path)
                build_result = self.build(buildspec_path)
                build_id = self.db.insert_build(buildspec, build_result, False)
                self.compare(pkg, build_id, build_result)

    def build(self, buildspec_path):
        """Given the path to a buildspec, runs the Reproducible Central rebuild.sh
        script in a subprocess, waiting for completion. Then it locates the .buildcompare
        file produced by the script to get the (non-)reproducible files and
        returns a Build_Result object.
        """
        # process = subprocess.run(["./rebuild.sh", buildspec_path]) # interactive mode
        process = subprocess.run(
            ["./rebuild.sh", buildspec_path],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )

        dir_path = os.path.dirname(buildspec_path)
        search_pattern = os.path.join(dir_path, "*.buildcompare")
        files = glob.glob(search_pattern)
        if len(files) == 0:
            return Build_Result(False, process.stdout.decode(), process.stderr.decode(), None, None)
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
                ok_files, ko_files = result.get("okFiles"), result.get("koFiles")
            return Build_Result(
                True,
                process.stdout.decode(),
                process.stderr.decode(),
                ok_files,
                ko_files,
            )

        except (FileNotFoundError, KeyError):
            self.log.debug("File not found or malformed. Build (probably) failed")
            return Build_Result(False, process.stdout.decode(), process.stderr.decode(), None, None)

    def buildspec_exists(self, pkg: PackageId) -> list:
        """Given a package, checks whether a buildspec has already been created by the Reproducible
        Central project and returns a list of all paths found.
        """
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
            pkg.groupid.replace(".", "/") + "/" + pkg.artifactid + "-" + pkg.version + ".buildspec"
        )
        path = os.path.join(base_path, relative_path)
        if pkg.artifactid == "7zip":
            self.log.debug(path)
        if os.path.exists(path):
            paths.append(path)
        return paths

    def create_buildspec(
        self, pkg: PackageId, git_repo, git_tag, tool, jdk, newline, command: str
    ) -> str:
        """Given build parameters, creates a buildspec using .buildspec.template
        saving it in "research/{pkg.groupid}-{pkg.artifactid}-{pkg.version}/"
        """
        values = {
            "groupId": pkg.groupid,
            "artifactId": pkg.artifactid,
            "version": pkg.version,
            "gitRepo": git_repo,
            "gitTag": git_tag,
            "tool": tool,
            "jdk": jdk,
            "newline": newline,
            "command": command.format(artifactId=pkg.artifactid)
            if "{artifactId}" in command
            else command,
        }
        with open(os.path.join(os.getcwd(), "..", "..", ".buildspec.template"), "r") as file:
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

        self.log.info(f"groupId = {groupId}")
        self.log.info(f"artifactId = {artifactId}")
        self.log.info(f"version = {version}")
        self.log.info(f"tool = {tool}")
        self.log.info(f"jdk = {jdk}")
        self.log.info(f"newline = {newline}")
        self.log.info(f"command = {command}")
        return Build_Spec(groupId, artifactId, version, tool, jdk, newline, command)

    def compare(self, pkg: PackageId, build_id: str, build_result: Build_Result):
        # base_path = f"research/{pkg.groupid}-{pkg.artifactid}-{pkg.version}/buildcache/{pkg.artifactid}/target/"
        base_path = f"research/{pkg.groupid}-{pkg.artifactid}-{pkg.version}/"

        if not build_result.build_success:
            self.log.debug("Build fail. Cannot compare JARs!")
            return
        non_repr_jars = [
            fname for fname in build_result.ko_files if os.path.splitext(fname)[1] == ".jar"
        ]
        if not non_repr_jars:
            self.log.debug("No non-reproducible JARs.")
            return

        search_pattern = os.path.join(base_path, "*.buildcompare")
        files = glob.glob(search_pattern)
        if len(files) == 0:
            # TODO LOG ERROR to DB
            self.db.insert_error(pkg, None, "(COMPARE) .buildcompare not found")
            return
        buildinfo = files[0]

        for jar in non_repr_jars:
            reference_path, actual_path = extract_path_buildinfo(pkg, jar, buildinfo)
            if reference_path is None or actual_path is None:
                self.db.insert_error(
                    pkg, None, "(COMPARE) Reference or Actual artifact path not found!"
                )
                return
            try:
                hash_mismatches, extra_files, missing_files = compare_jars(
                    actual_path, reference_path
                )
                self.db.insert_jar_repr(build_id, jar, hash_mismatches, missing_files, extra_files)
                self.log.debug(hash_mismatches)
                self.log.debug(extra_files)
                self.log.debug(missing_files)
            except FileNotFoundError:
                self.db.insert_error(pkg, None, "(COMPARE) Couldn't find one of the archives!")
                return

    def choose_jdk_versions(self, jdk_src_ver: str, pub_date, lts_only: bool) -> list:
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

        all_vers_after = dict(dropwhile(lambda kv: kv[0] != jdk_src_ver, jdk_rel_dates.items()))
        vers_at_publish = dict(takewhile(lambda kv: kv[1] < pub_date, all_vers_after.items()))
        if lts_only:
            return [ver for ver in vers_at_publish if ver in [jdk_src_ver, "8", "11", "17", "21"]]
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

    def parse_build_jdk(self, version):
        """Parses the major JDK version from the highly specific format
        returned by the java.version system property.
        """
        if not version:
            return None

        result = re.search(r"(?:(1\.\d)|[2-9](?=\.\d)|(\d{2}|\d{1}(?![\d\.])))", version)
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
        process = subprocess.run(["git", "clone", url, clone_dir], check=False)
        if process.returncode != 0:
            self.log.error("Problem encountered")
