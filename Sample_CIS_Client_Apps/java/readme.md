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
       "baseUrl": "https://your-cis-endpoint.com",
       "apiKey": "your-api-key"
   }
   ```

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
