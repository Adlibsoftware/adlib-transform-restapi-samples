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
       "baseUrl": "https://your-cis-endpoint.com",
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

## Project Structure
- `src`: Contains the main application code.
  - `apiClient.ts`: Handles API requests (main API integration file).
  - `config.ts`: Manages configuration settings.
  - `main.ts`: Entry point of the application.
  - `models.ts`: Defines data models for API responses.
- `appsettings.json`: Configuration file for the application.
- `package.json`: Contains project metadata and dependencies.
- `tsconfig.json`: TypeScript configuration file for the sameple app.
