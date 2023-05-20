import subprocess

from database import *
from utils import *

class VerifyHost:

    def __init__(self, db: Database):
        self.db = db
        self.TABLE = 'hosts'
        self.timeout = 30
        self.funcs = [lambda x : x, scm_to_url, git_to_https, http_to_https]

    # TODO add to errortable
    # TODO mark them as processed
    # TODO write tests to try different formats
    def verify_hosts(self):
        records = self.db.get_all()
        valid = 0

        for i, record in enumerate(records):
            print(f'{i}/{len(records)}')
            success = False
            print('-'*50)
            url = record['url']

            for func in self.funcs:
                if self.try_with(url, func):
                    success = True
                    break
            
            if success:
                valid += 1

        print(f'There were {valid} repos out of {len(records)}')


    def run_cmd(self, url: str):
        return subprocess.run(['env', 'GIT_TERMINAL_PROMPT=0', 'git', 'ls-remote', url, 'HEAD'], 
                             stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=self.timeout)
    

    def try_with(self, url: str, convert) -> bool:
        try:
           url = convert(url)
           print(f"Trying with {url}")
           process = self.run_cmd(url)
        except subprocess.TimeoutExpired:
            print(f'TIMED OUT after {self.timeout}s!')
            return False

        output = process.stdout.decode()
        err = process.stderr.decode()
        print(output)
        print(f'Exit code: {process.returncode}')
        if not err:
            return True
        else:
            print(err)
            return False
        


# Exceptions:

# git@gitee.com:fluent-mybatis/generator.git DOESNT WORK
# BUT
# https://gitee.com/fluent-mybatis/generator.git WORKS

# https://github.com/Auties00/noise-java/tree/master/ DOESNT WORK
# BUT
# https://github.com/Auties00/noise-java/ WORKS

# http needs to be transformed to https, otherwise github rejects

# scm:git:git@github.com:jitsni/jcifs.git we need to get rid of scm:git:

# http://github.com:jlangch/venice/tree/master HAS TYPO?

# git://github.com/instaclustr/instaclustr-icarus.git TIMES OUT
# BUT
# https://github.com/instaclustr/instaclustr-icarus.git WORKS


# Trying with https://github.com/cerner/ccl-testing/tree/master/ftp-util
# Exit code: 128
# remote: Please upgrade your git client.
# remote: GitHub.com no longer supports git over dumb-http: https://github.com/blog/809-git-dumb-http-transport-to-be-turned-off-in-90-days
# fatal: unable to access 'https://github.com/cerner/ccl-testing/tree/master/ftp-util/': The requested URL returned error: 403

# Ideas:
# - Try scm_url, homepage_url, etc (all regex'd) until something hits







    
        
    

