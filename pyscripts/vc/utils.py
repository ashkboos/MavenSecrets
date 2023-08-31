import re
from giturlparse import parse
from psycopg2.extras import DictRow
import zipfile, hashlib


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


def calculate_md5(file_content):
    md5 = hashlib.md5()
    md5.update(file_content)
    return md5.hexdigest()


def get_jar_content_hashes(jar_path):
    content_hashes: dict[str, str] = {}
    with zipfile.ZipFile(jar_path, "r") as jar:
        for file_info in jar.infolist():
            if file_info.filename.endswith("/"):  # Skip directories
                continue
            with jar.open(file_info.filename) as file:
                content = file.read()
                md5_hash = calculate_md5(content)
                content_hashes[file_info.filename] = md5_hash
    print(jar_path)
    print(content_hashes)
    return content_hashes


def compare_jars(artifact_path, reference_path):
    artifact_hashes = get_jar_content_hashes(artifact_path)
    reference_hashes = get_jar_content_hashes(reference_path)

    diff_md5 = []
    not_in_reference, not_in_artifact = [], []

    for file, md5_hash in artifact_hashes.items():
        if file not in reference_hashes:
            not_in_reference.append(file)

    for file, md5_hash in reference_hashes.items():
        if file not in artifact_hashes:
            not_in_artifact.append(file)
        elif md5_hash != reference_hashes[file]:
            diff_md5.append(file)

    return diff_md5, not_in_reference, not_in_artifact
