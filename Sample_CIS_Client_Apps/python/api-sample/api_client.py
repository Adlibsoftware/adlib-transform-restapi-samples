import aiohttp
import aiofiles
import os
from typing import List
from uuid import UUID
from datetime import datetime
from models import EnvironmentResponse, JobStatusResponse, RepositoryDto, GlobalVariableDto
from urllib.parse import unquote_to_bytes

class ApiClient:
    def __init__(
        self, base_url: str, api_key_header: str, api_key: str, skip_certificate_verification: bool = False
    ):
        self._session = aiohttp.ClientSession(
            connector=aiohttp.TCPConnector(ssl=not skip_certificate_verification)
        )
        self._api_key_header = api_key_header
        self._api_key = api_key
        self._base_path = (
            f"{base_url.rstrip('/')}/api/v2/ClientIntegration/"
            if base_url.endswith("/")
            else f"{base_url}/api/v2/ClientIntegration/"
        )

    async def close(self):
        await self._session.close()

    async def _add_api_key_header(self, headers: dict):
        headers[self._api_key_header] = self._api_key

    async def get_environment(self) -> EnvironmentResponse:
        headers = {}
        await self._add_api_key_header(headers)
        async with self._session.get(f"{self._base_path}Environment", headers=headers) as response:
            response.raise_for_status()
            data = await response.json()
            return EnvironmentResponse.model_validate(data)

    async def submit(self, repository_id: UUID, input_file_paths: List[str]) -> UUID:
        form = aiohttp.FormData(quote_fields=False)
        form.add_field("RepositoryId", str(repository_id))

        for i, file_path in enumerate(input_file_paths):
            async with aiofiles.open(file_path, "rb") as f:
                file_data = await f.read()
            form.add_field(
                f"InputFiles[{i}].InputFile",
                file_data,
                filename=os.path.basename(file_path),
                content_type="application/octet-stream",
            )

            # This doesn't need to be added, but shows how to add metadata
            form.add_field(f"InputFiles[{i}].FileMetadata[0].Name", "Python Sample App Submission")
            form.add_field(f"InputFiles[{i}].FileMetadata[0].Value", "Test file uploaded via Python sample app")

            '''
            # If you want to add metadata per file, you can do it like this:
            metadata = [
                [  # Metadata for first file
                    {"Name": "Author", "Value": "John Doe"},
                    {"Name": "Department", "Value": "Finance"},
                ],
                [  # Metadata for second file
                    {"Name": "Author", "Value": "Jane Smith"}
                ]
            ]
            try:
                for j, item in enumerate(metadata[i]):
                    form.add_field(f"InputFiles[{i}].FileMetadata[{j}].Name", item["Name"])
                    form.add_field(f"InputFiles[{i}].FileMetadata[{j}].Value", item["Value"])
            except IndexError:
                pass
                
            '''

        headers = {}
        await self._add_api_key_header(headers)
        async with self._session.post(f"{self._base_path}Submit", data=form, headers=headers) as response:
            response.raise_for_status()
            data = await response.json()
            return UUID(data)

    async def get_status(self, job_id: UUID) -> JobStatusResponse:
        headers = {}
        await self._add_api_key_header(headers)
        async with self._session.get(f"{self._base_path}Status/{job_id}", headers=headers) as response:
            response.raise_for_status()
            data = await response.json()
            return JobStatusResponse.model_validate(data)

    async def download(self, job_id: UUID, download_directory: str):
        headers = {}
        await self._add_api_key_header(headers)
        async with self._session.get(f"{self._base_path}Download/{job_id}", headers=headers) as response:
            response.raise_for_status()
            file_name = self._get_download_file_name(response.headers.get("Content-Disposition"), job_id)
            file_path = os.path.join(download_directory, file_name)
            async with aiofiles.open(file_path, "wb") as f:
                await f.write(await response.read())

    def _get_download_file_name(self, content_disposition: str | None, job_id: UUID) -> str:
        file_name = None

        if content_disposition:
            file_name_star = self._get_content_disposition_parameter(content_disposition, "filename*")

            if file_name_star:
                file_name = self._decode_rfc5987_value(file_name_star)

            if not file_name:
                file_name = self._get_content_disposition_parameter(content_disposition, "filename")

        if not file_name:
            file_name = f"{job_id}.unknown"

        return os.path.basename(file_name)

    def _get_content_disposition_parameter(self, header: str, parameter_name: str) -> str | None:
        lower_parameter_name = parameter_name.lower()

        for part in self._split_content_disposition_header(header)[1:]:
            if "=" not in part:
                continue

            name, value = part.split("=", 1)
            if name.strip().lower() == lower_parameter_name:
                return self._unquote_header_value(value.strip())

        return None

    def _split_content_disposition_header(self, header: str) -> list[str]:
        parts: list[str] = []
        current: list[str] = []
        in_quotes = False
        escaped = False

        for char in header:
            if escaped:
                current.append(char)
                escaped = False
            elif char == "\\" and in_quotes:
                escaped = True
            elif char == '"':
                in_quotes = not in_quotes
                current.append(char)
            elif char == ";" and not in_quotes:
                parts.append("".join(current).strip())
                current = []
            else:
                current.append(char)

        parts.append("".join(current).strip())
        return parts

    def _unquote_header_value(self, value: str) -> str:
        if len(value) >= 2 and value.startswith('"') and value.endswith('"'):
            return value[1:-1]

        return value

    def _decode_rfc5987_value(self, value: str) -> str | None:
        parts = value.split("'", 2)
        if len(parts) != 3:
            return None

        charset, _, encoded_value = parts
        try:
            return unquote_to_bytes(encoded_value).decode(charset or "utf-8")
        except (LookupError, UnicodeDecodeError):
            return None

    async def release(self, job_id: UUID):
        headers = {}
        await self._add_api_key_header(headers)
        async with self._session.put(f"{self._base_path}Release/{job_id}", headers=headers) as response:
            response.raise_for_status()
