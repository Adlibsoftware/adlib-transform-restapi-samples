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
       "baseUrl": "https://your-cis-endpoint.com",
       "apiKey": "your-api-key"
   }
   ```

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
