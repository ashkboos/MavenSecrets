from psycopg2.extensions import connection
from psycopg2.extras import DictCursor
from database import *
import re

class VerifyHost:

    def __init__(self, db):
        self.TABLE = 'hosts'
    
    def verify_hosts(self):
        print("verifying...")
    
        
    

