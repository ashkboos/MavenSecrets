from time import sleep
from giturlparse import parse
import requests

from database import Database

class CompareBuilds:

    def __init__(self, db: Database):
        self.db = db

    def find_github_release(self):
        records = self.db.get_all()
        url: str = 'https://api.github.com/repos/{0}/{1}/releases'
        for record in records:
            version = record['version']
            url = record['url']

            # TODO Make new query that does this
            host = record['hostname']
            if host != 'github.com':
                continue

            print(url)
            try:
                p = parse(url)
            except Exception as e:
               print(e) 
            if p.valid:
                print('REPO INFO:')
                print(p.host, p.owner,p.name)
            else:
                print('invalid')
                continue
            
            res = requests.get(f'https://api.github.com/repos/{p.owner}/{p.name}/releases')
            print(res.text)
            sleep(0.5)
            
