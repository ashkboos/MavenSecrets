from time import sleep
from giturlparse import parse
import requests
from dotenv import dotenv_values

from database import Database

class CompareBuilds:

    def __init__(self, db: Database):
        self.db = db
        self.env = dotenv_values()
        self.rate_lim_remain = 5000
        self.rate_lim_reset = None

    def find_github_release(self):
        records = self.db.get_all()
        for record in records:

            version = record['version']
            url = record['url']

            # TODO Make new SQL query that does this
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

            if self.rate_lim_remain < 2:
                 # TODO wait until self.rate_lim_reset before making next request
                 print("Waiting for rate limit!")
                 sleep(60)

            res = self.make_request(p.owner, p.name, version)

            if (res.status_code != 200):
                print(f"Bad status code received ({res.status_code})!")
                continue
            
            data = res.json()['data']
            self.rate_lim_remain = data['rateLimit']['remaining']
            self.rate_lim_reset = data['rateLimit']['resetAt']

            tag_exists = len(data['repository']['refs']['nodes']) != 0
            if not tag_exists:
                # TODO look in releases
                print("Checking releases")

            else:
                # TODO mark it as found
                commit_hash = data['repository']['refs']['nodes'][0]['target']['oid']
                print(f"Tag {version} with commit hash {commit_hash} FOUND!")
            
            sleep(0.1)


    def make_request(self, owner: str, repo: str, version: str):
        token = self.env.get("TOKEN")
        query ='''
        query ($owner: String!, $repo: String!, $version: String!) {
          rateLimit {
            cost
            remaining
            resetAt
          }
          repository(owner: $owner, name: $repo) {
            refs(refPrefix: "refs/tags/", query: $version, first: 1) {
              nodes {
                name
                target {
                  oid
                }
              }
            }
            releases(last: 100) {
              nodes {
                name
                tag {
                  name
                }
                tagCommit {
                  oid
                }
              }
            }
          }
        }
        '''
        variables = {
            'owner': owner,
            'repo': repo,
            'version': version
        }
        payload = {
            "query": query,
            "variables": variables
        }
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

        return requests.post("https://api.github.com/graphql", json=payload, headers=headers)
        
            
