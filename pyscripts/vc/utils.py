import re


# https://maven.apache.org/scm/scm-url-format.html
# https://maven.apache.org/scm/git.html
def git_to_https(url: str) -> tuple:
    pattern = r"(git://|git@)([^:]*):(.*)"
    replacement = r"https://\2/\3"
    n_url = re.sub(pattern, replacement, url)
    return (n_url, url != n_url)


# TODO look into what happens with repositories like username/tree/tree/master
def remove_tree_path(url: str) -> tuple:
    n_url = re.sub(r"/tree.*", "", url)
    return (n_url, url != n_url)


def remove_scm_prefix(url: str) -> str:
    return re.sub(
        r"^scm:(git@|git:)", lambda x: x.group(1) if x.group(1) != "git:" else "", url
    )
