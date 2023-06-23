import re
from giturlparse import parse


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
