class PackageId:

    def __init__(self, groupid: str, artifactid: str, version: str) -> None:
        self.groupid = groupid
        self.artifactid = artifactid
        self.version = version