import subprocess
import re

from database import *

class VerifyHost:

    def __init__(self, db: Database):
        self.db = db
        self.TABLE = 'hosts'
        self.timeout = 30
        self.funcs = [lambda x : x, lambda x: self.git_to_https(x), lambda x: self.http_to_https(x)]

    # TODO add to errortable
    # TODO mark them as processed
    def verify_hosts(self):
        records = self.db.get_all()
        valid = 0

        for record in records:
            success = False
            print('-'*50)
            url = record['url']
            print(url)

            for func in self.funcs:
                if self.tryWith(url, func):
                    success = True
            
            if success:
                valid += 1

        
        print(f'There were {valid} repos out of {len(records)}')


    def run_cmd(self, url: str):
        return subprocess.run(['env', 'GIT_TERMINAL_PROMPT=0', 'git', 'ls-remote', url, 'HEAD'], 
                             stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=self.timeout)
    

    def tryWith(self, url: str, convert):
        try:
           url = convert(url)
           process = self.run_cmd(url)
        except subprocess.TimeoutExpired:
            print(f'TIMED OUT after {self.timeout}s!')

        output = process.stdout.decode()
        err = process.stderr.decode()
        print(output)
        print(f'Exit code: {process.returncode}')
        if not err:
            return True
        else:
            print(err)
            return False


    def git_to_https(self, url: str) -> str:
        n_url = re.sub(r"git://", "https://", url)
        n_url = re.sub(r"git@", "https://", n_url)
        return n_url


    def http_to_https(self, url: str) -> str:
        pass




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






    
        
    

