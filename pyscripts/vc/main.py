import logging

from database import Database
from extract_repos import Extractor
from analyse_historical import HistoricalAnalyzer
from get_tags import GetTags
from build_packages import BuildPackages
from verify_hosts import VerifyHost


def main():
    logging.basicConfig(
        level=logging.DEBUG,
        format="%(asctime)s.%(msecs)03d [%(threadName)s] %(levelname)s %(name)s  - %(message)s",
        datefmt="%H:%M:%S",
    )

    log = logging.getLogger(__name__)
    log.info(f"Executing...)")

    db = Database("localhost", "5432", "postgres", "SuperSekretPassword")
    fields = ["host", "host_home", "host_scm_conn", "host_dev_conn"]

    extractor = Extractor(db)
    verifier = VerifyHost(db)
    analyser = HistoricalAnalyzer(db, fields)
    tag_finder = GetTags(db)
    builder = BuildPackages(db)

    # extractor.extract()
    # verifier.verify_hosts()
    # analyser.analyse_all()
    # tag_finder.find_github_release()
    builder.clone_rep_central()
    builder.build_and_compare()
    # Don't forget to note down the last commit of the cloned Reproducible Central repo


if __name__ == "__main__":
    main()
