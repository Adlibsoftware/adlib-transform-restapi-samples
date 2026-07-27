# CIS 2.0 Python Client

## Purpose

This project provides a sample Python client application for interacting with the CIS 2.0 REST API. It demonstrates how to configure, authenticate, and make API calls to the CIS 2.0 system. The project is designed to help developers understand how to integrate their Python applications with the CIS 2.0 platform.

## Features

- API client implementation for CIS 2.0.
- Configuration management via `appsettings.json`.
- Example usage of the API client in the `main.py` file.
- Dependency management using `requirements.txt`.

## Prerequisites

- Python 3.10 or higher.
- A valid CIS 2.0 API key (configured through the Transform UI) and endpoint.

## Setup

1. Install the required dependencies:

   ```bash
   pip install -r requirements.txt
   ```
2. Open the `appsettings.json` file and update the configuration values to use your endpoint and API key:

   ```json
   {
       "base_url": "https://your-transform-host-name-here/Adlib/ClientIntegrationService",
       "api_key": "your-api-key"
   }
   ```

## Submit By Reference (non-streaming)

By default the sample uploads (streams) the files in the `Input` folder. Set `UseSubmitByReference` to `true` in `appsettings.json` to instead submit files **by reference**: the API is given UNC paths or http(s) URIs, and the engine reads the inputs and writes the output directly, so there is no upload and no Download step.

```json
{
    "UseSubmitByReference": true,
    "ReferenceInputs": [
        { "Path": "\\\\your-file-server\\share\\input1.pdf" },
        { "Uri": "https://your-host/files/input2.docx" }
    ],
    "ReferenceOutput": { "Folder": "\\\\your-file-server\\share\\Output", "FileName": "result.pdf" },
    "ReferenceJobMetadata": []
}
```

- Each entry in `ReferenceInputs` uses either `Path` (UNC/local) or `Uri` (http/https); the two may be mixed. Optional per-input `Metadata` is passed through to that input.
- `ReferenceOutput` is either a `Folder` (with optional `FileName`) or a `Uri`; the output is written there directly, so there is no Download.
- `ReferenceJobMetadata` is an optional flat list of job-level name/value pairs (for example overlay or watermark references).
- `RepositoryId` is optional for this endpoint; when omitted the API uses the first repository the key is authorized for.
- The Transform engine service account must have read access to the referenced inputs and write access to the output destination.
- Flow: Environment then SubmitByReference then Status then Release (no Download).

## Run the Application

1. Run the application:
   ```bash
   python main.py
   ```

## Project Structure

- `api-sample/`: Contains the main application code.
  - `api_client.py`: Handles API requests (main API integration file).
  - `config.py`: Manages configuration settings.
  - `main.py`: Entry point of the application.
  - `models.py`: Defines data models for API responses.
- `appsettings.json`: Configuration file for the application.
- `requirements.txt`: File containing Python dependencies.
