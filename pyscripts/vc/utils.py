import re

# https://maven.apache.org/scm/scm-url-format.html
# https://maven.apache.org/scm/git.html
# TODO convert scm urls
def git_to_https(url: str) -> str:
    n_url = re.sub(r"^git://", "https://", url)
    n_url = re.sub(r"^git@", "https://", n_url)
    return (n_url, url != n_url)


def http_to_https(url: str) -> str:
    n_url = re.sub(r'^http://', 'https://', url)
    return (n_url, url != n_url) 
    
def scm_to_url(url: str) -> str:
    # scm:git:git://server_name[:port]/path_to_repository
    # scm:git:http://server_name[:port]/path_to_repository
    # scm:git:https://server_name[:port]/path_to_repository
    # scm:git:ssh://server_name[:port]/path_to_repository

    # TODO still need to try converting git_to_https
    print("SCM")
    n_url = re.sub(r"^scm:git@", "git@", url)
    n_url = re.sub(r"^scm:git:", "", n_url)
    n_url = re.sub(r"^git://", "https://", n_url)
    return (n_url, url != n_url)