import hashlib
import os
import re
import zipfile

from common.packageId import PackageId
from giturlparse import parse
from psycopg2.extras import DictRow


# https://maven.apache.org/scm/scm-url-format.html
# https://maven.apache.org/scm/git.html
def git_or_ssh_to_https(url: str) -> tuple:
    pattern = r"(git://|git@)([^:]*):(.*)"
    replacement = r"https://\2/\3"
    n_url = re.sub(pattern, replacement, url)
    return (n_url, url != n_url)


def git_to_https(url: str) -> str:
    n_url = re.sub(r"^git://", "https://", url)
    return (n_url, url != n_url)


def add_https_if_missing(url: str) -> str:
    pattern = re.compile(r"^(https?|git|ssh)://|^git@|^ssh@")
    if not pattern.match(url):
        url = "https://" + url
        return (url, True)
    else:
        return (url, False)


# TODO look into what happens with repositories like username/tree/tree/master
def remove_tree_path(url: str) -> tuple:
    n_url = re.sub(r"/tree.*", "", url)
    return (n_url, url != n_url)


def remove_scm_prefix(url: str) -> str:
    return re.sub(
        r"^scm:(git@|git:)", lambda x: x.group(1) if x.group(1) != "git:" else "", url
    )


def convert_link_to_github(url: str) -> str:
    pattern = re.compile(r"([\w-]+\.git)")
    match = pattern.search(url)
    if match:
        repo_name = match.group(1).replace(".git", "")
        return f"https://github.com/apache/{repo_name}"
    else:
        pattern = r"https?://[\w-]+\.apache\.org/repos/asf/(\w+)"
        replacement = r"https://github.com/apache/\1"
        return re.sub(pattern, replacement, url)


# Replaces http with https, removes trailing slashes
# and adds .git to git@ urls to make it work with parsing lib
def parse_plus(url: str):
    url = re.sub(r"\/+$", "", url)
    if re.match(r"^git@", url) and not re.search(r"\.git$", url):
        return parse(url + ".git")
    https_url = re.sub(r"http:", "https:", url)
    return parse(https_url)


def get_field(record: DictRow, field_name: str, mandatory: bool = False):
    """Gets database record value in a given field. Can return None if
    mandatory = False and value in field is null.

    Throws:
    ValueError if value in field is null,
    KeyError if field_name not present in record
    """
    val = record[field_name]
    if mandatory and not val:
        raise ValueError(f"{field_name} is null")
    else:
        return val


def calculate_sha512(file_content):
    sha512 = hashlib.sha512()
    sha512.update(file_content)
    return sha512.hexdigest()


def get_jar_file_hashes(jar_path):
    file_hashes: dict[str, str] = {}
    with zipfile.ZipFile(jar_path, "r") as jar:
        for file_info in jar.infolist():
            if file_info.filename.endswith("/"):  # Skip directories
                continue
            with jar.open(file_info.filename) as file:
                content = file.read()
                sha512 = calculate_sha512(content)
                file_hashes[file_info.filename] = sha512
    print(jar_path)
    return file_hashes


def compare_jars(actual_path, reference_path):
    actual_hashes = get_jar_file_hashes(actual_path)
    reference_hashes = get_jar_file_hashes(reference_path)

    hash_mismatches = []
    extra_files, missing_files = [], []

    for file, sha512 in reference_hashes.items():
        if file not in actual_hashes:
            missing_files.append(file)
        elif sha512 != actual_hashes[file]:
            hash_mismatches.append(file)

    for file, sha512 in actual_hashes.items():
        if file not in reference_hashes:
            extra_files.append(file)

    return hash_mismatches, extra_files, missing_files


def extract_path_buildinfo(pkg: PackageId, artifact_name, buildinfo):
    """Get .buildcompare, extract only lines like "# diffoscope {reference_path} {target_path}"
    For each line, check whether our jar is a substring, if yes extract reference and target paths.
    """
    base_path = os.path.join(os.path.dirname(buildinfo), "buildcache", pkg.artifactid)
    with open(buildinfo, "r") as file:
        for line in file:
            if line.startswith("# diffoscope"):
                if artifact_name in line:
                    parts = line.strip().split(" ")
                    reference_path = parts[2]
                    actual_path = parts[3]
                    return os.path.join(base_path, reference_path), os.path.join(
                        base_path, actual_path
                    )
    return None, None
