import yaml
import logging


class Config:
    def __init__(self) -> None:
        self.log = logging.getLogger(__name__)
        config = self.load_config("config.yaml")

        self.LOG_LEVEL = config["log_level"]
        self.DB_CONFIG = config["database"]
        self.RUN_LIST = [
            key
            for dictionary in config["run_config"]
            for key, value in dictionary.items()
            if value
        ]
        self.GITHUB_API_KEY = config["github_api_key"]
        if self.GITHUB_API_KEY is None:
            self.log.warn("GITHUB API KEY NOT SET!")

    def load_config(self, filename):
        with open(filename) as config_file:
            config = yaml.safe_load(config_file)
        return config
