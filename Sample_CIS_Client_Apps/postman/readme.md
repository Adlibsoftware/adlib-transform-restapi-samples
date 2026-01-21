# Client Integration Service (CIS) 2.0 API - Postman Collection

This Postman collection provides comprehensive testing capabilities for the Adlib Client Integration Service (CIS) v2.0 REST API. The API enables client applications to submit files for processing, monitor job status, and retrieve results through a complete document transformation workflow.

## Overview

The Client Integration Service v2.0 API provides endpoints for:
- **Environment Management**: Retrieve available repositories and global variables
- **Job Submission**: Upload files with metadata for processing
- **Status Monitoring**: Track job progress and completion
- **Result Retrieval**: Download processed files and metadata
- **Job Management**: Release, cancel, and update job information

## Base URL Structure

All endpoints follow the pattern: `{{baseUrl}}/api/v2/ClientIntegration/{endpoint}`

## Authentication

The API uses API Key authentication with the following methods:
- **Header**: `X-Api-Key: {{apiKey}}`

## Environment Variables

Configure these variables in your Postman environment:

| Variable | Description | Example |
|----------|-------------|---------|
| `baseUrl` | Base URL of the API server | `https://your-server.com` |
| `apiKey` | Your API key for authentication | `your-api-key-here` |
| `jobId` | GUID of a specific job | `f978c559-a016-420f-9427-de4308cebb35` |

## API Endpoints

### 1. Environment
- **Method**: `GET`
- **Endpoint**: `/Environment`
- **Description**: Retrieves all available repositories and global variables for the authenticated API key
- **Response**: List of repositories with workspace information and global variables
- **Use Case**: Initial setup to discover available processing repositories

### 2. Statistics
- **Method**: `GET`
- **Endpoint**: `/Statistics/{repositoryId}`
- **Description**: Returns job processing statistics for a specific repository
- **Parameters**: 
  - `repositoryId` (GUID): The repository to get statistics for
- **Response**: Count of jobs by status (Queued, Transforming, Completed, etc.) and average processing times
- **Use Case**: Monitor repository performance and workload

### 3. Status
- **Method**: `GET`
- **Endpoint**: `/Status/{jobId}`
- **Description**: Retrieves detailed status information for a specific job
- **Parameters**:
  - `jobId` (GUID): The job to check status for
- **Response**: Job status, processing details, and timing information
- **Use Case**: Monitor individual job progress

### 4. Completed
- **Method**: `GET`
- **Endpoint**: `/Completed/{repositoryId}`
- **Parameters**:
  - `repositoryId` (GUID): Repository to filter completed jobs
  - `maxItems` (optional): Maximum number of jobs to return (default: 10)
- **Description**: Lists all completed jobs that haven't been released yet
- **Response**: Array of completed job details
- **Use Case**: Retrieve finished jobs ready for download

### 5. Info
- **Method**: `GET`
- **Endpoint**: `/Info/{jobId}`
- **Description**: Gets comprehensive information about a job including input/output files and all metadata
- **Parameters**:
  - `jobId` (GUID): The job to get detailed information for
- **Response**: Complete job information with file details and metadata
- **Use Case**: Detailed job inspection and metadata review

### 6. Download
- **Method**: `GET`
- **Endpoint**: `/Download/{jobId}`
- **Description**: Downloads the output file(s) from a completed job
- **Parameters**:
  - `jobId` (GUID): The job to download files from
- **Response**: 
  - Single file: Returns the file directly
  - Multiple files: Returns a ZIP archive containing all files
- **Use Case**: Retrieve processed results

### 7. Submit
- **Method**: `POST`
- **Endpoint**: `/Submit`
- **Content-Type**: `multipart/form-data`
- **Description**: Submits files for processing with optional metadata
- **Form Data Parameters**:
  - `RepositoryId`: Target repository GUID
  - `InputFiles[0].InputFile`: Primary input file(s)
  - `InputFiles[0].FileMetadata[0].Name`: File-specific metadata name
  - `InputFiles[0].FileMetadata[0].Value`: File-specific metadata value
  - `SupportingFiles`: Additional supporting files (optional)
  - `JobMetaData[0].Name`: Job-level metadata name
  - `JobMetaData[0].Value`: Job-level metadata value
- **Response**: New job GUID
- **Use Case**: Start new processing jobs

### 8. Release
- **Method**: `PUT`
- **Endpoint**: `/Release/{jobId}`
- **Description**: Marks a completed job as released/delivered
- **Parameters**:
  - `jobId` (GUID): The job to mark as released
- **Response**: HTTP 204 No Content on success
- **Use Case**: Releases the job for the system after you are done with it (clean up).

### 9. Cancel
- **Method**: `PUT`
- **Endpoint**: `/Cancel/{jobId}`
- **Description**: Cancels a queued job (only works if job hasn't started processing)
- **Parameters**:
  - `jobId` (GUID): The job to cancel
- **Response**: HTTP 204 No Content on success
- **Use Case**: Cancel jobs that are no longer needed or are stuck

### 10. Metadata
- **Method**: `PUT`
- **Endpoint**: `/Metadata/{jobId}`
- **Content-Type**: `application/json`
- **Description**: Updates job and file metadata for an existing job
- **Request Body**:
```json
{
    "fileMetadata": [
        {
            "fileName": "Sample_Invoice.pdf",
            "metadata": [
                {
                    "name": "Test",
                    "value": "Testing1234"
                }
            ]
        }
    ],
    "jobMetadata": [
        {
            "name": "Test",
            "value": "Test1"
        }
    ]
}
```
- **Response**: HTTP 204 No Content on success
- **Use Case**: Update metadata after job submission

## Typical Workflow

1. **Discovery**: Call `Environment` to get available repositories
2. **Submission**: Use `Submit` to upload files and start processing
3. **Monitoring**: Poll `Status` to track job progress
4. **Completion**: Check `Completed` for finished jobs
5. **Retrieval**: Use `Info` to get details, then `Download` to get files
6. **Cleanup**: Call `Release` to mark jobs as delivered

## Response Formats

### Success Response Structure
```json
{
    "success": true,
    "message": "",
    // ... additional response data
}
```

### Error Response Structure
```json
{
    "success": false,
    "message": "Error description"
}
```

## Status Codes

- **200 OK**: Successful request
- **204 No Content**: Successful operation with no response body
- **400 Bad Request**: Invalid parameters or request format
- **401 Unauthorized**: Invalid or missing API key
- **404 Not Found**: Job, repository, or resource not found

## Example Responses

### Environment Response
```json
{
    "repositories": [
        {
            "id": "68ac01e1-263b-4437-8b59-f1c40a07610a",
            "name": "Production Repository",
            "type": "Standard",
            "workspaceId": "12345678-1234-1234-1234-123456789012",
            "workspaceName": "Production Workspace"
        }
    ],
    "globalVariables": [
        {
            "key": "ProcessingMode",
            "value": "Standard"
        }
    ],
    "lastChanged": "2025-09-23T10:30:00Z",
    "success": true,
    "message": ""
}
```

### Job Info Response
```json
{
    "jobId": "f978c559-a016-420f-9427-de4308cebb35",
    "repositoryId": "68ac01e1-263b-4437-8b59-f1c40a07610a",
    "status": "CompletedSuccessful",
    "details": "Processing successful with PdfProcessing.Engine",
    "totalQueueTimeInSec": 101.4,
    "totalProcessingTimeInSec": 117.2,
    "inputFiles": [
        {
            "fileName": "700pagesforMRCtest.pdf",
            "fileLength": 948760471,
            "metadata": [
                {
                    "name": "Test",
                    "value": "130MB doc"
                }
            ]
        }
    ],
    "outputFiles": [
        {
            "fileName": "700pagesforMRCtest.pdf",
            "fileLength": 29615940,
            "metadata": []
        }
    ],
    "success": true,
    "message": ""
}
```

## Notes

- All file uploads support large files (up to 4GB)
- Jobs remain in the system until explicitly released or the internal time auto releases them.
- Supporting files are optional and copied to the same work folder as input files
- Metadata can be attached at both job and file levels
- The API supports both single file and batch processing scenarios

## Support

For API issues or questions, consult your system administrator or Adlib support documentation.