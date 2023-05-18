from database import Database
from extract_repos import Extractor
from verify_hosts import VerifyHost

def main():
    db = Database('localhost', '5432', 'postgres', 'SuperSekretPassword')
    extractor = Extractor(db)
    verifier = VerifyHost(db) 

    extractor.extract()
    verifier.verify_hosts()

if __name__ == "__main__": 
  main()

