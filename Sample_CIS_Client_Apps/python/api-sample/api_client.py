import aiohttp
import aiofiles
import os
from typing import List
from uuid import UUID
from datetime import datetime
from models import EnvironmentResponse, JobStatusResponse, RepositoryDto, GlobalVariableDto
from email.parser import Parser

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
        form = aiohttp.FormData()
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
            content_disposition = response.headers.get("Content-Disposition")
            if content_disposition:
                parsed = Parser().parsestr(f"Content-Disposition: {content_disposition}")
                file_name = parsed.get_filename()
                if file_name is None:
                    file_name = f"{job_id}.unknown"
            else:
                file_name = f"{job_id}.unknown"
            file_path = os.path.join(download_directory, file_name)
            async with aiofiles.open(file_path, "wb") as f:
                await f.write(await response.read())

    async def release(self, job_id: UUID):
        headers = {}
        await self._add_api_key_header(headers)
        async with self._session.put(f"{self._base_path}Release/{job_id}", headers=headers) as response:
            response.raise_for_status()