import aiohttp
import asyncio
import os
import time
from datetime import datetime
from typing import List
from uuid import UUID
import sys
from config import Config, load_config
from api_client import ApiClient
from models import EnvironmentResponse, JobStatusResponse, SubmitByReferenceRequest

LOG_FILE_PATH = "log.txt"
JOB_LOG_FILE_PATH = "JobLogs"
INPUT_DIRECTORY = "Input"
DOWNLOAD_DIRECTORY = "Output"


async def main():
    config = load_config()
    if config is None:
        return

    if not startup_tasks(config):
        return

    input_files = [os.path.join(INPUT_DIRECTORY, f) for f in os.listdir(INPUT_DIRECTORY) if os.path.isfile(os.path.join(INPUT_DIRECTORY, f))]

    client = ApiClient(config.base_url, config.api_key_header, config.api_key, config.trust_certs)

    try:
        print("SETTINGS")
        print(f"Url: {config.base_url}")
        print(f"Submit as Separate Jobs: {'Yes' if config.separate_jobs else 'No'}")
        print(f"Skip Cert Verification: {'Yes' if config.trust_certs else 'No'}")
        print(f"Polling Rate: {config.polling_rate_seconds}s")

        input("\nPress enter to start...")

        if config.use_submit_by_reference:
            # Non-streaming: submit inputs by UNC path / URI from config; the engine writes output
            # directly to the configured destination, so there is no Download step.
            await process_by_reference_job(client, config)
        else:
            # 1. Get Environment
            env = await client.get_environment()
            if not env.repositories:
                raise Exception("No repositories available.")
            repository_id = env.repositories[0].id
            print(f"Using repository: {env.repositories[0].name} (ID: {repository_id})")

            if config.separate_jobs and len(input_files) > 1:
                print("Submitting as multiple jobs.\n")
                # Process each file as a separate job in parallel using asyncio.gather
                tasks = []
                for i, file in enumerate(input_files):
                    tasks.append(process_job(client, repository_id, [file], i, config))
                await asyncio.gather(*tasks)
            else:
                print("Submitting as same job.\n")
                # Process all files as one job
                await process_job(client, repository_id, input_files, -1, config)

        print("Demo Completed Successfully.")
    except aiohttp.ClientResponseError as ex:
        handle_error(config, f"API error: {ex.message}. Status: {ex.status}")
    except Exception as ex:
        handle_error(config, f"Error: {ex}")
    finally:
        await client.close()

    print("Press enter to close...")
    input()


async def process_job(client: ApiClient, repository_id: UUID, files: List[str], id: int = -1, config: Config = None):
    # 2. Submit files
    submit_message = (
        f"Submitting file: {os.path.basename(files[0])}..."
        if len(files) == 1
        else f"Submitting {len(files)} files ({', '.join([os.path.basename(f) for f in files])}) "
    )
    log(submit_message, id)

    job_id = await client.submit(repository_id, files)
    log(f"Submitted. Job ID: {job_id}\n", id)

    # 3. Poll Status
    while True:
        await asyncio.sleep(config.polling_rate_seconds)
        status = await client.get_status(job_id)
        log(f"Status: {status.status}. ID: {job_id}", id)
        if status.status.startswith("Completed"):
            break

    if status.status != "CompletedSuccessful":
        raise Exception(f"Job completed with status: {status.status}. Details: {status.details}")
    log(f"Job {job_id} completed successfully.\n", id)

    # 4. Download
    location = os.path.join(DOWNLOAD_DIRECTORY, str(job_id))
    os.makedirs(location, exist_ok=True)

    log(f"Downloading files from Job: {job_id}", id)
    await client.download(job_id, location)
    log(f"Download complete. Location: {location}\n", id)

    # 5. Release
    log(f"Releasing Job: {job_id}", id)
    await client.release(job_id)
    log("Job Released.\n", id)


async def process_by_reference_job(client: ApiClient, config: Config):
    # Submits a job by reference: inputs are named by UNC path or URI (from appsettings.json), the engine
    # writes output directly to the configured destination, then we poll status and release. No Download step.
    if not config.reference_inputs:
        raise Exception("UseSubmitByReference is true but ReferenceInputs is empty. Add input path/uri references to appsettings.json.")

    # 1. Get Environment (RepositoryId is optional for SubmitByReference; we resolve one here for clarity).
    env = await client.get_environment()
    if not env.repositories:
        raise Exception("No repositories available.")
    repository_id = env.repositories[0].id
    print(f"Using repository: {env.repositories[0].name} (ID: {repository_id})")

    # 2. Submit by reference (no file bytes are uploaded)
    submit_request = SubmitByReferenceRequest(
        repositoryId=repository_id,
        inputs=config.reference_inputs,
        output=config.reference_output,
        metadata=config.reference_job_metadata or [],
    )
    log(f"Submitting {len(submit_request.inputs)} input reference(s) by reference...", -1)
    job_id = await client.submit_by_reference(submit_request)
    log(f"Submitted. Job ID: {job_id}\n", -1)

    # 3. Poll Status
    while True:
        await asyncio.sleep(config.polling_rate_seconds)
        status = await client.get_status(job_id)
        log(f"Status: {status.status}. ID: {job_id}", -1)
        if status.status.startswith("Completed"):
            break

    if status.status != "CompletedSuccessful":
        raise Exception(f"Job completed with status: {status.status}. Details: {status.details}")
    log(f"Job {job_id} completed successfully. Output was written directly to the configured destination.\n", -1)

    # 4. Release (no Download - the engine wrote output directly to the destination)
    log(f"Releasing Job: {job_id}", -1)
    await client.release(job_id)
    log("Job Released.\n", -1)


def startup_tasks(config: Config) -> bool:
    with open(LOG_FILE_PATH, "w", encoding="utf-8") as f:
        f.write("")

    os.makedirs(INPUT_DIRECTORY, exist_ok=True)
    os.makedirs(JOB_LOG_FILE_PATH, exist_ok=True)
    # Clear job log directory
    try:
        for file in os.listdir(JOB_LOG_FILE_PATH):
            os.remove(os.path.join(JOB_LOG_FILE_PATH, file))
    except Exception as ex:
        print(f"Error clearing {JOB_LOG_FILE_PATH} directory: {ex}")

    if not config.use_submit_by_reference:
        input_files = [f for f in os.listdir(INPUT_DIRECTORY) if os.path.isfile(os.path.join(INPUT_DIRECTORY, f))]
        if not input_files:
            handle_error(config, "No files in Input folder to submit. Exiting.")
            return False

    return True


def log(message: str, id: int):
    now = datetime.now().isoformat()
    if id == -1:
        print(message)
        with open(os.path.join(JOB_LOG_FILE_PATH, "joblog.txt"), "a", encoding="utf-8") as f:
            f.write(f"{now}: {message.strip()}\n")
    else:
        print(f"Thread: {id}, {message}")
        with open(os.path.join(JOB_LOG_FILE_PATH, f"joblog_{id}.txt"), "a", encoding="utf-8") as f:
            f.write(f"{now}: {message.strip()}\n")


def handle_error(config: Config, message: str):
    print(message)
    with open(LOG_FILE_PATH, "a", encoding="utf-8") as f:
        f.write(f"{datetime.now().isoformat()}: {message}\n")

    for i in range(config.error_close_seconds, -1, -1):
        print(f"\rClosing in {i} seconds...", end="")
        if i != 0:
            time.sleep(1)
    print()
    sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())