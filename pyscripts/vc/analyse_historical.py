
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

from database import Database

class HistoricalAnalyzer:

    def __init__(self, db: Database):
        self.db = db


    def analyse(self):
        cmap = 'viridis'
        threshold = 0.05

        records = self.db.collate_hosts_yearly()
        # records = self.gen_test_data()
        df = pd.DataFrame(records, columns=['host','count', 'year'])
        df['market_share'] = df.groupby('year')['count'].transform(lambda x: x / x.sum())

        below = df[df['market_share'] < threshold]
        others = below.groupby('year').agg({'host': lambda x: ' '.join(['others']), 'count': sum, 'market_share': sum}).reset_index()
        df = pd.concat([df, others])
        df = df[~df.isin(below)].dropna()

        pivot_df = df.pivot(index='year', columns='host', values='market_share')
        pivot_df.plot(kind='area', stacked=True, colormap=cmap)
        plt.show()

    
    def gen_test_data(self):
        hosts = ['github.com', 'gitlab.com', 'gitee.com', 'gitlab.alibaba-inc.com', 
                 'bitbucket.org', 'gitbucket.com', 'git.com', 'git.us', 'gitman.sm']

        data = []
        for year in range(2011,2024):
            for host in list(set(hosts)):
                count =  int(1000 * np.random.random_sample())
                if np.random.binomial(1,0.25) == 1:
                    count = None
                if host == 'github.com':
                    count = 5000
                entry = [host, count,year]
                data.append(entry)
        return data 