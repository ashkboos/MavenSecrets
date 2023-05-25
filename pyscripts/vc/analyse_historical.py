
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

from database import Database

class HistoricalAnalyzer:

    def __init__(self, db: Database):
        self.db = db


    def analyse(self):
        # records = self.db.collate_hosts_yearly()
        records = self.gen_test_data()
        cmap = 'viridis'

        df = pd.DataFrame(records, columns=['host','count', 'year'])
        df['market share'] = df.groupby('year')['count'].transform(lambda x: x / x.sum())

        pivot_df = df.pivot(index='year', columns='host', values='market share')
        pivot_df.plot(kind='area', stacked=True, colormap=cmap)
        print(df)
        print(pivot_df)
        plt.show()
        
    
    def gen_test_data(self):
        hosts = ['github.com', 'gitlab.com', 'gitee.com', 'gitlab.alibaba-inc.com', 
                 'bitbucket.org', 'gitbucket.com', 'git.com', 'git.us', 'gitman.sm']

        data = []
        for year in range(2011,2023):
            print(year)
            for host in list(set(hosts)):
                count =  int(1000 * np.random.random_sample())
                entry = [host, count,year]
                data.append(entry)
        return data 