import os
import re


def create_build_spec_coord2path_dic(repo_path):
    with open(os.path.join(repo_path, 'README.md'), 'r') as file:
        content = file.read()

    table_content = re.search(r"<!-- BEGIN GENERATED RESULTS TABLE -->(.*?)<!-- END GENERATED RESULTS TABLE -->",
                              content, re.DOTALL).group(1)
    table_lines = table_content.strip().split('\n')[2:-1]  # Skip table headers and last row stats

    artifacts_dict = {}
    for line in table_lines:
        matches = re.search(r"\| ?(\S*?) ?\| <a name='(\S+?:\S+?)'></a>\[(\S+?)\]\((\S+?)\)", line)
        group, full_name, artifact, link = matches.groups()
        modules = {full_name}
        if link:
            artifact_readme_path = os.path.join(repo_path, link)
            with open(artifact_readme_path, 'r') as file:
                content = file.read()
            table_pattern = r"<details><summary>This project defines \d+ modules:</summary>(.*?)</details>"
            table = re.search(table_pattern, content, re.DOTALL)

            if table is not None:
                artifact_pattern = r"\[\s*([\w\.-]+:[\w\.-]+)\s*\]"
                artifact_matches = re.findall(artifact_pattern, table.group(1))
                modules.update(set(artifact_matches))

        versions = re.findall(r'\[\d+\.\d+\.\d+\]', content)
        clean_versions = [v.strip('[]') for v in versions]
        for version in clean_versions:
            artifact_path = link.replace("README.md", "")
            build_spec = find_file_with_suffix(os.path.join(repo_path, artifact_path), version + ".buildspec")
            for module in modules:
                artifacts_dict[module + ":" + version] = build_spec

    return artifacts_dict


def find_file_with_suffix(directory, suffix):
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(suffix):
                return os.path.join(root, file)
    return None


if __name__ == '__main__':
    repo_path = "/Users/mehdi/Desktop/MyMac/Phd/Repositories/reproducible-central"

    coord2path_dic = create_build_spec_coord2path_dic(repo_path)
    for coordinate in coord2path_dic:
        print(coordinate, ":", coord2path_dic[coordinate])
