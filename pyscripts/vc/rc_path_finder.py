import os
import re

from common.packageId import PackageId


def create_build_spec_coord2path_dic(repo_path):
    with open(os.path.join(repo_path, "README.md"), "r") as file:
        content = file.read()

    table_content = re.search(
        r"<!-- BEGIN GENERATED RESULTS TABLE -->(.*?)<!-- END GENERATED RESULTS TABLE -->",
        content,
        re.DOTALL,
    ).group(1)
    table_lines = table_content.strip().split("\n")[2:-1]  # Skip table headers and last row stats

    artifacts_dict = {}
    for line in table_lines:
        matches = re.search(r"\| ?(\S*?) ?\| <a name='(\S+?:\S+?)'></a>\[(\S+?)\]\((\S+?)\)", line)
        group, full_name, artifact, link = matches.groups()
        modules = {full_name}
        if link:
            artifact_readme_path = os.path.join(repo_path, link)
            with open(artifact_readme_path, "r") as file:
                content = file.read()
            table_pattern = (
                r"<details><summary>This project defines \d+ modules:</summary>(.*?)</details>"
            )
            table = re.search(table_pattern, content, re.DOTALL)

            if table is not None:
                artifact_pattern = r"\[\s*([\w\.-]+:[\w\.-]+)\s*\]"
                artifact_matches = re.findall(artifact_pattern, table.group(1))
                modules.update(set(artifact_matches))

        versions = re.findall(r"\[(\d+(\.\d+)?(\.\d+)?(-\w+)?(\.\w+)?)\]", content)
        clean_versions = [v[0].strip("[]") for v in versions]
        for version in clean_versions:
            artifact_path = link.replace("README.md", "")
            build_spec = remove_path_prefix(
                find_file_with_suffix(
                    os.path.join(repo_path, artifact_path), version + ".buildspec"
                ),
                repo_path,
            )
            for module in modules:
                artifacts_dict[module + ":" + version] = build_spec

    return artifacts_dict


def remove_path_prefix(buildspec_path: str | None, repo_path):
    if buildspec_path is None:
        return None
    if buildspec_path.startswith(repo_path):
        return buildspec_path[len(repo_path) :]


def find_file_with_suffix(directory, suffix) -> str | None:
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(suffix):
                return os.path.join(root, file)
    return None


if __name__ == "__main__":
    repo_path = "temp/builder/"

    coord2path_dic = create_build_spec_coord2path_dic(repo_path)
    for coordinate in coord2path_dic:
        print(coordinate, ":", coord2path_dic[coordinate])
    print(len(coord2path_dic))
    print(len({k:v for k,v in coord2path_dic.items() if v is not None}))
