from database import Database
from extract_repos import Extractor
from analyse_historical import HistoricalAnalyzer
from verify_hosts import VerifyHost


def main():
    db = Database("localhost", "5432", "postgres", "SuperSekretPassword")
    extractor = Extractor(db)
    verifier = VerifyHost(db)
    analyser = HistoricalAnalyzer(db)

    extractor.extract()
    verifier.verify_hosts()
    analyser.analyse()


if __name__ == "__main__":
    main()
