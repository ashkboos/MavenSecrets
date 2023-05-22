import subprocess

from database import *
from utils import *

class VerifyHost:

    def __init__(self, db: Database):
        self.db = db
        self.TABLE = 'hosts'
        self.timeout = 30
        self.funcs = [lambda x : x, scm_to_url, git_to_https, http_to_https]

    # TODO mark them as processed
    # TODO write tests to try different formats
    def verify_hosts(self):
        records = self.db.get_all()
        valid = 0

        for i, record in enumerate(records):
            print(f'{i}/{len(records)}')
            success = False
            errs = []
            print('-'*50)
            url = record['url']

            for convert_func in self.funcs:
                converted_url = convert_func(url)
                err = self.try_with(converted_url)
                if err is None:
                    success = True
                    break
                else:
                    errs.append(err)
                    # TODO add to errortable
            
            if success:
                valid += 1
            else:
                print(errs)

        print(f'There were {valid} repos out of {len(records)}')


    def run_cmd(self, url: str):
        return subprocess.run(['env', 'GIT_TERMINAL_PROMPT=0', 'git', 'ls-remote', url, 'HEAD'], 
                             stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=self.timeout)
    

    def try_with(self, url: str) -> str:
        try:
           print(f"Trying with {url}")
           process = self.run_cmd(url)
        except subprocess.TimeoutExpired:
            print(f'TIMED OUT after {self.timeout}s!')
            return 'TIMED OUT'

        output = process.stdout.decode()
        err = process.stderr.decode()
        print(output)
        print(f'Exit code: {process.returncode}')
        if process.returncode == 0:
            return None
        else:
            return err
        


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







    
        
    

