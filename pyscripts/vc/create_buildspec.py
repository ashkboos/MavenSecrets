import os

from jinja2 import Template
from common.packageId import PackageId
from build_packages import BuildPackages


def main():
    os.chdir("./temp/builder")
    builder = BuildPackages(None, None)
    pkg = PackageId("org.apache.maven.wagon", "wagon-http-lightweight", "3.5.0")
    path = builder.create_buildspec(
        pkg,
        "https://github.com/apache/maven-wagon",
        "wagon-3.5.0",
        "mvn",
        "8",
        "lf",
    )
    result = builder.build(path, pkg)
    print(result.stdout)
    print("------------ERRORS----------")
    print(result.stderr)


main()
