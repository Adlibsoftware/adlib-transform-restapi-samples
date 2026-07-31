# CIS 2.0 Java Client

## Purpose

This project provides a sample Java client application for interacting with the CIS 2.0 REST API. It demonstrates how to configure, authenticate, and make API calls to the CIS 2.0 system. The project is designed to help developers understand how to integrate their Java applications with the CIS 2.0 platform.

## Features

- API client implementation for CIS 2.0.
- Configuration management via `appsettings.json`.
- Example usage of the API client in the `Main` class.
- Maven-based build and dependency management.

## Prerequisites

- Java Development Kit (JDK) 8 or higher. Project was made with JDK 21 in mind.
- Maven 3.6 or higher.
- A valid CIS 2.0 API key (configured through the Transform UI) and endpoint.

## Setup

- Open the `appsettings.json` file and update the configuration values to use you endpoint and API key:
  ```json
  {
      "baseUrl": "https://your-transform-host-name-here/Adlib/ClientIntegrationService",
      "apiKey": "your-api-key"
  }
  ```

## Submit By Reference (non-streaming)

By default the sample streams files from the local `Input` folder to the API (`POST /Submit`) and downloads
results to `Output` (`GET /Download`). As an alternative, the sample can submit jobs **by reference**
(`POST /SubmitByReference`): instead of uploading file bytes, you name each input by a UNC path or an
`http(s)` URI, and the engine writes the output **directly** to a destination you specify. Because the engine
writes output itself, there is **no Download step** in this mode.

Enable it in `appsettings.json`:

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

Settings:

- `UseSubmitByReference` (boolean, default `false`): when `true`, the sample runs the by-reference flow
  (Environment -> SubmitByReference -> poll Status -> Release) and skips the local `Input` folder check.
- `ReferenceInputs` (list): each entry supplies **either** a `Path` (UNC or local path) **or** a `Uri`
  (`http(s)` URI). Use one per entry. An optional per-input `Metadata` list of `{ "Name": ..., "Value": ... }`
  pairs can be attached to that input's document.
- `ReferenceOutput` (object, optional): where output is written directly — a `Folder` (UNC or local) with an
  optional `FileName`, or a `Uri`.
- `ReferenceJobMetadata` (list, optional): job-level `{ "Name": ..., "Value": ... }` metadata pairs.

Notes:

- Inputs are UNC paths or `http(s)` URIs — the files are **not** uploaded from the local machine.
- Output is written directly to the configured destination, so the sample performs **no** Download.
- `RepositoryId` is optional; when omitted the API uses the first repository the API key is authorized for.
  (The sample resolves and sends the first available repository for clarity.)
- The **engine service account** must have read access to the input locations and write access to the output
  destination.

## Build and Run

1. Build the project using Maven:

   ```bash
   mvn clean install
   ```
2. Run the application:

   ```bash
   java -cp target/CIS-2.0-1.0-SNAPSHOT.jar Main
   ```

## Project Structure

- `src/main/java`: Contains the main application code.
  - `ApiClient.java`: Handles API requests (main api integration file).
  - `Config.java`: Manages configuration settings.
  - `Main.java`: Entry point of the application.
  - `Models.java`: Defines data models for API responses.
- `appsettings.json`: Configuration file for the application.
- `pom.xml`: Maven configuration file containing dependencies.
