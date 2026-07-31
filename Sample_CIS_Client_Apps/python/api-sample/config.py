import json
import os
from typing import List, Optional
from models import InputReference, OutputReference, MetadataDto

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
        use_submit_by_reference: bool = False,
        reference_inputs: Optional[List[InputReference]] = None,
        reference_output: Optional[OutputReference] = None,
        reference_job_metadata: Optional[List[MetadataDto]] = None,
    ):
        self.base_url = base_url
        self.api_key = api_key
        self.api_key_header = api_key_header
        self.error_close_seconds = error_close_seconds
        self.polling_rate_seconds = polling_rate_seconds
        self.separate_jobs = separate_jobs
        self.trust_certs = trust_certs
        # SubmitByReference (non-streaming) settings. When use_submit_by_reference is true, the sample submits the
        # reference_inputs (UNC paths or http(s) URIs) via POST /SubmitByReference and the engine writes output directly.
        self.use_submit_by_reference = use_submit_by_reference
        self.reference_inputs = reference_inputs if reference_inputs is not None else []
        self.reference_output = reference_output
        self.reference_job_metadata = reference_job_metadata if reference_job_metadata is not None else []


def _parse_metadata_list(raw) -> List[MetadataDto]:
    result: List[MetadataDto] = []
    if isinstance(raw, list):
        for item in raw:
            if isinstance(item, dict):
                result.append(MetadataDto(name=item.get("Name", ""), value=item.get("Value", "")))
    return result


def _parse_reference_inputs(raw) -> List[InputReference]:
    result: List[InputReference] = []
    if isinstance(raw, list):
        for item in raw:
            if isinstance(item, dict):
                result.append(InputReference(
                    path=item.get("Path"),
                    uri=item.get("Uri"),
                    metadata=_parse_metadata_list(item.get("Metadata")),
                ))
    return result


def _parse_reference_output(raw) -> Optional[OutputReference]:
    if isinstance(raw, dict):
        return OutputReference(
            folder=raw.get("Folder"),
            fileName=raw.get("FileName"),
            uri=raw.get("Uri"),
        )
    return None

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
            use_submit_by_reference=False,
        )
        try:
            with open(config_file, "w", encoding="utf-8") as f:
                json.dump({
                    "BaseUrl": default_config.base_url,
                    "ApiKey": default_config.api_key,
                    "ApiKeyHeader": default_config.api_key_header,
                    "ErrorCloseSeconds": default_config.error_close_seconds,
                    "PollingRateSeconds": default_config.polling_rate_seconds,
                    "SeparateJobs": default_config.separate_jobs,
                    "TrustCerts": default_config.trust_certs,
                    "UseSubmitByReference": default_config.use_submit_by_reference,
                    "ReferenceInputs": [
                        {"Path": "\\\\your-file-server\\share\\input1.pdf"},
                        {"Uri": "https://your-host/files/input2.docx"},
                    ],
                    "ReferenceOutput": {"Folder": "\\\\your-file-server\\share\\Output", "FileName": "result.pdf"},
                    "ReferenceJobMetadata": [],
                }, f, indent=4)
            print(f"Created default {config_file} with default values.")
        except Exception as ex:
            print(f"Failed to create {config_file}: {ex}")
            return None

    try:
        with open(config_file, "r", encoding="utf-8") as f:
            data = json.load(f)
        config = Config(
            base_url=data.get("BaseUrl", ""),
            api_key=data.get("ApiKey", ""),
            api_key_header=data.get("ApiKeyHeader", ""),
            error_close_seconds=data.get("ErrorCloseSeconds", 5),
            polling_rate_seconds=data.get("PollingRateSeconds", 7),
            separate_jobs=data.get("SeparateJobs", False),
            trust_certs=data.get("TrustCerts", False),
            use_submit_by_reference=data.get("UseSubmitByReference", False),
            reference_inputs=_parse_reference_inputs(data.get("ReferenceInputs")),
            reference_output=_parse_reference_output(data.get("ReferenceOutput")),
            reference_job_metadata=_parse_metadata_list(data.get("ReferenceJobMetadata")),
        )
        if (
            not config.base_url
            or not config.api_key
            or not config.api_key_header
            or config.error_close_seconds <= 0
            or config.polling_rate_seconds <= 0
        ):
            print(
                f"Invalid {config_file}. Ensure BaseUrl, ApiKey, ApiKeyHeader, ErrorCloseSeconds, and PollingRateSeconds are valid."
            )
            return None
        return config
    except Exception as ex:
        print(f"Failed to read or parse {config_file}: {ex}")
        return None