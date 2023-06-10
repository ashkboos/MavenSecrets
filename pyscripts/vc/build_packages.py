import logging
import subprocess
import os
from jinja2 import Template

from database import Database
from packageId import PackageId


class BuildPackages:
    def __init__(self, db: Database):
        self.log = logging.getLogger(__name__)
        self.db = db

    def build_and_compare(self):
        pkg = PackageId("com.github.hazendaz.7zip", "7zip", "23.00")

        values = {
            "groupId": "io.cucumber",
            "artifactId": "gherkin",
            "version": "26.2.0",
            "gitRepo": "https://github.com/cucumber/gherkin.git",
            "gitTag": "v26.2.0",
            "tool": "mvn-3.9.2",
            "jdk": "11",
            "newline": "lf",
        }

        if self.buildspec_exists(pkg):
            self.log.debug("Buildspec found!")

        # with open(".buildspec.template", "r") as file:
        #     content = file.read()
        # template = Template(content)
        # rendered = template.render(values)

        # path = f"./temp/builder/research/{values['groupId']}-{values['artifactId']}-{values['version']}/"
        # if not os.path.exists(path):
        #     os.makedirs(path)
        # with open(path + ".buildspec", "w") as file:
        #     file.write(rendered)

    def clone_rep_central(self):
        clone_dir = "pyscripts/vc/temp/builder"
        url = "https://github.com/jvm-repo-rebuild/reproducible-central.git"
        process = subprocess.run(["git", "clone", url, clone_dir])
        if process.returncode != 0:
            self.log.error("Problem encountered")

    def buildspec_exists(self, pkg: PackageId):
        base_path = "."  # assuming current directory
        relative_path = (
            pkg.groupid.replace(".", "/")
            + "/"
            + pkg.artifactid
            + "-"
            + pkg.version
            + ".buildspec"
        )

        # Combine the base path with the relative path
        path = os.path.join(base_path, relative_path)
        self.log.debug(path)
        return os.path.exists(path)

    def build(self):
        pass

    def compare(self):
        pass


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
