class Build_Spec:
    def __init__(
        self,
        groupid: str,
        artifactid: str,
        version: str,
        tool: str,
        jdk: str,
        newline: str,
        command: str,
    ) -> None:
        self.groupid = groupid
        self.artifactid = artifactid
        self.version = version
        self.tool = tool
        self.jdk = jdk
        self.newline = newline
        self.command = command
