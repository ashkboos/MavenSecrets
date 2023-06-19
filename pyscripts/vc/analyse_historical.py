import logging
import matplotlib.pyplot as plt
import matplotlib.ticker as mtick
import pandas as pd
import numpy as np

from database import Database
from common.config import Config


class HistoricalAnalyzer:
    def __init__(self, fields: list, db: Database, config: Config):
        self.log = logging.getLogger(__name__)
        plt.set_loglevel("warning")
        self.db = db
        self.config = config
        self.fields = fields

    def analyse_all(self):
        for field in self.fields:
            self.analyse(field)

    def analyse(self, field: str):
        cmap = "viridis"
        threshold = 0.01

        records = self.db.collate_hosts_yearly(field)
        # records = self.gen_test_data()
        df = pd.DataFrame(records, columns=["host", "count", "year"])
        df["market_share"] = df.groupby("year")["count"].transform(
            lambda x: x / x.sum()
        )

        below = df[df["market_share"] < threshold]
        if len(below) > 1:
            others = (
                below.groupby("year")
                .agg(
                    {
                        "host": lambda x: " ".join(["Others"]),
                        "count": sum,
                        "market_share": sum,
                    }
                )
                .reset_index()
            )
            df = pd.concat([df, others])
            df = df[~df.isin(below)].dropna()
        
        df = df.sort_values(by=['year','market_share'], ascending=[True,False])
        # print(df)

        pivot_df = df.pivot(index="year", columns="host", values="market_share")
        print(pivot_df)
        ax = pivot_df.plot.bar(stacked=True, colormap=cmap, width=0.95)
        ax.yaxis.set_major_formatter(mtick.PercentFormatter(1.0))
        ax.yaxis.set_major_locator(mtick.MultipleLocator(base=0.1))
        # # TODO save to file instead of showing
        # with open(f'data_{field}.csv', 'w') as f:
        #     pivot_df.to_csv(f)

        plt.show()

    def gen_test_data(self):
        hosts = [
            "github.com",
            "gitlab.com",
            "gitee.com",
            "gitlab.alibaba-inc.com",
            "bitbucket.org",
            "gitbucket.com",
            "git.com",
            "git.us",
            "gitman.sm",
        ]

        data = []
        for year in range(2011, 2024):
            for host in list(set(hosts)):
                count = int(1000 * np.random.random_sample())
                if np.random.binomial(1, 0.25) == 1:
                    count = None
                if host == "github.com":
                    count = 5000
                entry = [host, count, year]
                data.append(entry)
        return data
