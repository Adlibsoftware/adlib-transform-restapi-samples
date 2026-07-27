using System;
using System.Collections.Generic;


namespace CIS_2_0
{


    /// <summary>
    /// Base response status for all API responses.
    /// </summary>
    public class ResponseStatus
    {
        public bool Success { get; set; } = true;
        public string Message { get; set; } = string.Empty;
    }


    /// <summary>
    /// Response from the Environment endpoint including repositories and global variables.
    /// </summary>
    public class EnvironmentResponse : ResponseStatus
    {
        public List<RepositoryDto> repositories { get; set; } = new List<RepositoryDto>();
        public List<GlobalVariableDto> globalVariables { get; set; } = new List<GlobalVariableDto>();
        public DateTime? lastChanged { get; set; } = null;
    }


    /// <summary>
    /// A global variable key/value pair.
    /// </summary>
    public class GlobalVariableDto
    {
        public string Key { get; set; } = string.Empty;
        public string Value { get; set; } = string.Empty;
    }


    /// <summary>
    /// A metadata key/value pair.
    /// </summary>
    public class MetadataDto
    {
        public string Name { get; set; } = string.Empty;
        public string Value { get; set; } = string.Empty;
    }


    /// <summary>
    /// Response from the Job Status endpoint including job details and timing.
    /// </summary>
    public class JobStatusResponse : ResponseStatus
    {
        public Guid JobId { get; set; }
        public Guid RepositoryId { get; set; }
        public string Status { get; set; } = string.Empty;
        public string Details { get; set; } = string.Empty;
        public double TotalQueueTimeInSec { get; set; }
        public double TotalProcessingTimeInSec { get; set; }
    }


    /// <summary>
    /// A repository that can be submitted to including its workspace.
    /// </summary>
    public class RepositoryDto
    {
        public Guid Id { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Type { get; set; } = string.Empty;
        public Guid WorkspaceId { get; set; }
        public string WorkspaceName { get; set; } = string.Empty;
    }


    /// <summary>
    /// A single input file referenced by UNC/local path OR by http(s) URI (use one per entry).
    /// Optional per-input metadata is passed through to that input's document.
    /// </summary>
    public class InputReference
    {
        public string? Path { get; set; }
        public string? Uri { get; set; }
        public List<MetadataDto> Metadata { get; set; } = new List<MetadataDto>();
    }


    /// <summary>
    /// Where the engine writes output directly: a UNC/local Folder (+ optional FileName), or an http(s) Uri.
    /// </summary>
    public class OutputReference
    {
        public string? Folder { get; set; }
        public string? FileName { get; set; }
        public string? Uri { get; set; }
    }


    /// <summary>
    /// Request body for POST /SubmitByReference. Inputs are referenced by path/URI (not uploaded) and the
    /// engine writes output directly to the requested destination, so no Download call is needed.
    /// RepositoryId is optional; when omitted the API uses the first repository the key is authorized for.
    /// </summary>
    public class SubmitByReferenceRequest
    {
        public Guid? RepositoryId { get; set; }
        public List<InputReference> Inputs { get; set; } = new List<InputReference>();
        public OutputReference? Output { get; set; }
        public List<MetadataDto> Metadata { get; set; } = new List<MetadataDto>();
    }

}

