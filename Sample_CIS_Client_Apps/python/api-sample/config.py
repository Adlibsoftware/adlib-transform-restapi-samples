import json
import os
from typing import Optional

class Config:
    def __init__(
        self,
        base_url: str,
        api_key: str,
        api_key_header: str,
        error_close_seconds: int,
        polling_rate_seconds: int,
        separate_jobs: bool = False,
        trust_certs: bool = False,
    ):
        self.base_url = base_url
        self.api_key = api_key
        self.api_key_header = api_key_header
        self.error_close_seconds = error_close_seconds
        self.polling_rate_seconds = polling_rate_seconds
        self.separate_jobs = separate_jobs
        self.trust_certs = trust_certs

def load_config() -> Optional[Config]:
    config_file = "appsettings.json"
    if not os.path.exists(config_file):
        # Create default config
        default_config = Config(
            base_url="https://localhost:60204",
            api_key="your-api-key-here",
            api_key_header="X-Api-Key",
            error_close_seconds=5,
            polling_rate_seconds=7,
            separate_jobs=False,
            trust_certs=False,
        )
        try:
            with open(config_file, "w", encoding="utf-8") as f:
                json.dump(default_config.__dict__, f, indent=4)
            print(f"Created default {config_file} with default values.")
        except Exception as ex:
            print(f"Failed to create {config_file}: {ex}")
            return None

    try:
        with open(config_file, "r", encoding="utf-8") as f:
            data = json.load(f)
        config = Config(
            base_url=data.get("base_url", ""),
            api_key=data.get("api_key", ""),
            api_key_header=data.get("api_key_header", ""),
            error_close_seconds=data.get("error_close_seconds", 5),
            polling_rate_seconds=data.get("polling_rate_seconds", 7),
            separate_jobs=data.get("separate_jobs", False),
            trust_certs=data.get("trust_certs", False),
        )
        if (
            not config.base_url
            or not config.api_key
            or not config.api_key_header
            or config.error_close_seconds <= 0
            or config.polling_rate_seconds <= 0
        ):
            print(
                f"Invalid {config_file}. Ensure base_url, api_key, api_key_header, error_close_seconds, and polling_rate_seconds are valid."
            )
            return None
        return config
    except Exception as ex:
        print(f"Failed to read or parse {config_file}: {ex}")
        return None