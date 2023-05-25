from database import Database
from extract_repos import Extractor
from analyse_historical import HistoricalAnalyzer
from compare_builds import CompareBuilds
from verify_hosts import VerifyHost


def main():
    db = Database("localhost", "5432", "postgres", "SuperSekretPassword")
    extractor = Extractor(db)
    verifier = VerifyHost(db)
    analyser = HistoricalAnalyzer(db)
    comparer = CompareBuilds(db)

    extractor.extract()
    verifier.verify_hosts()
    analyser.analyse()
    comparer.find_github_release()


if __name__ == "__main__":
    main()
