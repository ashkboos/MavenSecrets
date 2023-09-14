import os

from build_packages import BuildPackages
from common.packageId import PackageId


def main():
    """
    Standalone script used to build individual packages with given buildspec parameters
    (DEBUGGING).
    """
    os.chdir("./temp/builder")
    builder = BuildPackages(None, None)
    pkg = PackageId("io.github.git-commit-id", "git-commit-id-maven-plugin", "6.0.0")
    path = builder.create_buildspec(
        pkg,
        "https://github.com/git-commit-id/git-commit-id-maven-plugin.git",
        "v6.0.0",
        "mvn",
        "11",
        "lf",
        "mvn clean package"
    )
    result = builder.build(path)
    print(result.stdout)
    print("------------ERRORS----------")
    print(result.stderr)


main()
