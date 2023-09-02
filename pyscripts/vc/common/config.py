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
        if "tag_finder" in self.RUN_LIST and self.GITHUB_API_KEY is None:
            raise ValueError("GITHUB API KEY NOT SET!")
        self.BUILD_CMD: str = config["build_cmd"]
        if "builder" in self.RUN_LIST and not self.BUILD_CMD:
            raise ValueError("Build command not set in config. Builder will FAIL!")

    def load_config(self, filename):
        with open(filename) as config_file:
            config = yaml.safe_load(config_file)
        return config
