class PackageId:
    def __init__(self, groupid: str, artifactid: str, version: str) -> None:
        self.groupid = groupid
        self.artifactid = artifactid
        self.version = version

    def __repr__(self) -> str:
        return f"{self.groupid}:{self.artifactid}:{self.version}"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, PackageId):
            return False
        return (self.groupid, self.artifactid, self.version) == (
            other.groupid,
            other.artifactid,
            other.version,
        )

    def __hash__(self) -> int:
        return hash((self.groupid, self.artifactid, self.version))
