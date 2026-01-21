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
       "base_url": "https://your-cis-endpoint.com",
       "api_key": "your-api-key"
   }
   ```

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
