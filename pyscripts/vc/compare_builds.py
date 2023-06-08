import logging
from time import sleep
from giturlparse import parse
import requests
import subprocess
from dotenv import dotenv_values
from typing import Dict

from database import Database

class CompareBuilds:

    def __init__(self, db: Database):
        self.log = logging.getLogger(__name__)
        logging.getLogger("urllib3").setLevel(logging.WARNING)
        self.db = db
        self.env = dotenv_values()
        self.rate_lim_remain = 5000
        self.rate_lim_reset = None
        

    # TODO try each field until 1 hits
    def find_github_release(self):
        field = 'valid'
        records = self.db.get_valid_urls(field)
        for record in records:

            version = record['version']
            url = record['url']

            print(url)
            try:
                p = parse(url)
            except Exception as e:
               self.log.error(e)
            if p.valid:
                self.log.debug(f'REPO INFO: {p.host}, {p.owner}, {p.name}')
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
            
            json = res.json()
            data: Dict = json['data']

            self.rate_lim_remain = data['rateLimit']['remaining']
            self.rate_lim_reset = data['rateLimit']['resetAt']
            self.log.debug(f'Rate Lim Remaining: {self.rate_lim_remain}')

            try:
                tag_exists = len(data['repository']['refs']['nodes']) > 0
            except Exception as e:
              self.log.exception(e)
              self.log.error(f'Probably response did not contain all fields! Request: ({p.owner},{p.name},{version})')
              continue
            if not tag_exists:
                # TODO pagination
              try:
                  releases = data['repository']['releases']['nodes']
                  if len(releases) > 0:
                    matches = [rel for rel in releases if rel.get("name") == version]
                    for rel in matches:
                      rel_name = rel['name']
                      tag_name = rel['tag']['name']
                      commit_hash = rel['tagCommit']
                      self.log.debug(f'Release {rel_name} with Tag {tag_name} found for Version {version}!')

              except Exception as e:
                  self.log.exception(e)
                  continue
            else:
                # TODO mark it as found
                commit_hash = data['repository']['refs']['nodes'][0]['target']['oid']
                tag_name = data['repository']['refs']['nodes'][0]['name']
                self.log.debug(f"Version {version} with Tag {tag_name} and commit hash {commit_hash} FOUND!")
            
            sleep(0.1)

    
    def build_and_compare(self):
        # TODO replace with only github repos that have a matching tag
        records = self.db.get_all()
        clone_dir = './clones'
        for record in records:
            url = record['url']
            process = subprocess.run(['git', 'clone', url, clone_dir])
            if process.returncode != 0:
                print('Problem encountered')
                continue
            

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
        
            
