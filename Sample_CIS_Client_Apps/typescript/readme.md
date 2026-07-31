# CIS 2.0 TypeScript Client

## Purpose

This project provides a sample TypeScript client application for interacting with the CIS 2.0 REST API. It demonstrates how to configure, authenticate, and make API calls to the CIS 2.0 system. The project is designed to help developers understand how to integrate their TypeScript applications with the CIS 2.0 platform.

## Features

- API client implementation for CIS 2.0.
- Configuration management via `appsettings.json`.
- Example usage of the API client in the `main.ts` file.

## Prerequisites

- Node.js 16 or higher.
- npm (Node Package Manager).
- A valid CIS 2.0 API key (configured through the Transform UI) and endpoint.

## Setup

- Open the `appsettings.json` file and update the configuration values to use your endpoint and API key:
  ```json
  {
      "baseUrl": "https://your-transform-host-name-here/Adlib/ClientIntegrationService",
      "apiKey": "your-api-key"
  }
  ```

## Build and Run

1. Install dependencies:

   ```bash
   npm install
   ```
2. Build the project:

   ```bash
   npm run build
   ```
3. Run the application:

   ```bash
   npm start
   ```

## Submit By Reference (non-streaming)

By default the sample streams (uploads) the files in the `Input` folder to the API and later
downloads the results. As an alternative, the sample supports a **non-streaming** mode that submits
input files *by reference* using `POST /api/v2/ClientIntegration/SubmitByReference`. In this mode no
bytes are uploaded: the API is given UNC/local paths or http(s) URIs, and the engine reads the inputs
and **writes the output directly** to the destination you configure. Because output is written
directly, there is **no Download step**.

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

- **`UseSubmitByReference`**: set to `true` to use the by-reference flow instead of streaming. When
  `true`, the `Input` folder is not required and its "must contain files" precheck is skipped.
- **`ReferenceInputs`**: one entry per input. Each entry uses either `Path` (a UNC or local path) **or**
  `Uri` (an http(s) URL) — you can mix the two across entries. An optional per-input `Metadata` array
  (`[{ "Name": "...", "Value": "..." }]`) may be supplied.
- **`ReferenceOutput`**: where the engine writes the result. Use `Folder` (+ optional `FileName`) for a
  UNC/local destination, or `Uri` for an http(s) destination.
- **`ReferenceJobMetadata`**: optional job-level metadata (`[{ "Name": "...", "Value": "..." }]`), e.g.
  overlay/watermark settings.
- **`RepositoryId` is optional** for `SubmitByReference`; when omitted the API uses the first
  repository the API key is authorized for. This sample resolves and sends one for clarity.

The by-reference flow is: `Environment` -> `SubmitByReference` -> poll `Status` until it completes
(fails unless `CompletedSuccessful`) -> `Release`. There is no `Download` call.

> **Note:** The Transform engine (service) account must have **read access** to the referenced input
> paths/URIs and **read/write access** to the output destination, since the engine — not this client —
> reads the inputs and writes the output.

## Project Structure

- `src`: Contains the main application code.
  - `apiClient.ts`: Handles API requests (main API integration file).
  - `config.ts`: Manages configuration settings.
  - `main.ts`: Entry point of the application.
  - `models.ts`: Defines data models for API responses.
- `appsettings.json`: Configuration file for the application.
- `package.json`: Contains project metadata and dependencies.
- `tsconfig.json`: TypeScript configuration file for the sameple app.
