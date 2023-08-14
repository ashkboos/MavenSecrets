import logging

from common.config import Config
from database import Database
from extract_hosts import Extractor
from analyse_historical import HistoricalAnalyzer
from get_tags import GetTags
from build_packages import BuildPackages
from verify_hosts import VerifyHost


def main():
    config = Config()
    logging.basicConfig(
        level=config.LOG_LEVEL,
        format="%(asctime)s.%(msecs)03d [%(threadName)s] %(levelname)s %(name)s  - %(message)s",
        datefmt="%H:%M:%S",
    )
    log = logging.getLogger(__name__)
    log.info(f"Executing with log level {config.LOG_LEVEL}...")

    db = Database(
        config.DB_CONFIG["hostname"],
        config.DB_CONFIG["port"],
        config.DB_CONFIG["username"],
        config.DB_CONFIG["password"],
    )
    fields = ["host", "host_home", "host_scm_conn", "host_dev_conn"]

    if "extractor" in config.RUN_LIST:
        log.info("Running Extractor...")
        extractor = Extractor(db, config)
        extractor.extract()

    if "verifier" in config.RUN_LIST:
        log.info("Running Verifier...")
        verifier = VerifyHost(db, config)
        verifier.verify_hosts()

    if "analyser" in config.RUN_LIST:
        log.info("Running Analyser...")
        analyser = HistoricalAnalyzer(fields, db, config)
        analyser.analyse_all()

    if "tag_finder" in config.RUN_LIST:
        log.info("Running Tag Finder...")
        tag_finder = GetTags(db, config)
        tag_finder.find_tags()

    if "builder" in config.RUN_LIST:
        log.info("Running Builder...")
        builder = BuildPackages(db, config)
        builder.clone_rep_central()
        builder.build_and_compare()

    # Don't forget to note down the last commit of the cloned Reproducible Central repo


if __name__ == "__main__":
    main()
