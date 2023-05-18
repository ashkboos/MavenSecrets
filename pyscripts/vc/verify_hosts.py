import subprocess
import git

from psycopg2.extensions import connection
from psycopg2.extras import DictCursor
from database import *

class VerifyHost:

    def __init__(self, db: Database):
        self.db = db
        self.TABLE = 'hosts'
        self.timeout = 30

    
    def verify_hosts(self):
        records = self.db.get_all()
        valid = 0

        for record in records:
            print('-'*50)
            url = record['url']
            print(url)

            try:
                process = subprocess.run(['env', 'GIT_TERMINAL_PROMPT=0', 'timeout', '30', 'git', 'ls-remote', url, 'HEAD'], 
                                     stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=self.timeout)
            except TimeoutError:
                print(f'TIMED OUT after {self.timeout}s!')

            output = process.stdout.decode()
            err = process.stderr.decode()
            print(output)
            if not err:
                valid += 1
            else:
                # Check if private or not
                print(err)

            print(f'Exit code: {process.returncode}')
        
        print(f'There were {valid} repos out of {len(records)}')


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
# https://github.com/instaclustr/instaclustr-icarus.git DOESNT WORK






    
        
    

