import logging

from database import Database
from extract_repos import Extractor
from analyse_historical import HistoricalAnalyzer
from compare_builds import CompareBuilds
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
    fields = ['host', 'host_home', 'host_scm_conn', 'host_dev_conn']

    extractor = Extractor(db)
    verifier = VerifyHost(db)
    analyser = HistoricalAnalyzer(db, fields)
    comparer = CompareBuilds(db)

    extractor.extract()
    verifier.verify_hosts()
    analyser.analyse_all()
    comparer.find_github_release()


if __name__ == "__main__":
    main()
