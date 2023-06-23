from typing import List


class Build_Result:
    def __init__(
        self,
        build_success: str,
        stdout: str,
        stderr: str,
        ok_files: List[str],
        ko_files: List[str],
    ) -> None:
        self.build_success = build_success
        self.stdout = stdout
        self.stderr = stderr
        self.ok_files = ok_files
        self.ko_files = ko_files
