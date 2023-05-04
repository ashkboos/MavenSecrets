import psycopg2
import psycopg2.extras


class Database:
    def __init__(self, host, port, user, password):
        self.host = host
        self.port = port
        self.user = user
        self.password = password

    def show(self):
        pass

    def connect(self):
        return psycopg2.connect(
            dbname='postgres',
            user=self.user,
            password=self.password,
            host=self.host,
            port=self.port
        )
