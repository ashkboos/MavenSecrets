import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed

from database import *
from utils import *
from packageId import PackageId

class VerifyHost:

    def __init__(self, db: Database):
        self.db = db
        self.timeout = 30
        self.funcs = [lambda x : (x, True), scm_to_url, git_to_https, remove_tree_path]

    def verify_hosts(self):
        records = self.db.get_all()

        with ThreadPoolExecutor() as executor:
            futures = [executor.submit(self.verify_single_host, record) for record in records]
            for future in as_completed(futures):
                success = future.result()
            print("All done. Thread Pool shutting down...")


    def verify_single_host(self, record):
        success = False
        print('-'*50)

        urls = [record['url'], record['url_home'], record['url_scm_conn'], record['url_dev_conn']]
        valid_fields = ['valid', 'valid_home', 'valid_scm_conn', 'valid_dev_conn']

        pkg = PackageId(record['groupid'], record['artifactid'], record['version'])

        for j, url in enumerate(urls): 
            if url is None:
                continue

            for convert_func in self.funcs:
                converted_url, changed = convert_func(url)
                if not changed:
                    continue 

                err = self.try_with(converted_url)
                if err is None:
                    success = True
                    self.db.update_validity(valid_fields[j], pkg, converted_url)
                    print('VALID:', valid_fields[j])
                    break
                else:
                    self.db.insert_error(pkg, converted_url, err)
        
        self.db.mark_processed(pkg)
        return success


    def run_cmd(self, url: str):
        return subprocess.run(['env', 'GIT_TERMINAL_PROMPT=0', 'GIT_SSH_COMMAND=ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no',
                               'git', 'ls-remote', url, 'HEAD'], 
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
