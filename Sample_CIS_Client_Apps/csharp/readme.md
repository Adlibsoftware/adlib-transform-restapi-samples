# CIS 2.0 C# Client

## Purpose

This project provides a sample C# client application for interacting with the CIS 2.0 REST API. It demonstrates how to configure, authenticate, and make API calls to the CIS 2.0 system. The project is designed to help developers understand how to integrate their C# applications with the CIS 2.0 platform.

## Features

- API client implementation for CIS 2.0.
- Configuration management via `appsettings.json`.
- Example usage of the API client in the `Program.cs` file.
- .NET 8.0-based project.

## Prerequisites

- .NET SDK 8.0 or higher.
- A valid CIS 2.0 API key (configured through the Transform UI) and endpoint.

## Setup

1. Open the `appsettings.json` file and update the configuration values to use your endpoint and API key:
   ```json
   {
       "baseUrl": "https://your-transform-host-name-here/Adlib/ClientIntegrationService",
       "apiKey": "your-api-key"
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
- `ReferenceOutput` is either a `Folder` (with optional `FileName`) or a `Uri`.
- `ReferenceJobMetadata` is an optional flat list of job-level name/value pairs (for example overlay or watermark references).
- `RepositoryId` is optional for this endpoint; when omitted the API uses the first repository the key is authorized for.
- The Transform engine service account must have read access to the referenced inputs and write access to the output destination.
- Flow: Environment then SubmitByReference then Status then Release (no Download).

## Build and Run

Open project file with Visual Studio OR

1. Build the project using the .NET CLI:

   ```bash
   dotnet build
   ```
2. Run the application:

   ```bash
   dotnet run
   ```

## Project Structure

- `CIS 2.0/`: Contains the main application code
  - `ApiClient.cs`: Handles API requests (main API integration file).
  - `Config.cs`: Manages configuration settings.
  - `Models.cs`: Defines data models for API responses.
  - `Program.cs`: Entry point of the application.
- `appsettings.json`: Configuration file for the application.
