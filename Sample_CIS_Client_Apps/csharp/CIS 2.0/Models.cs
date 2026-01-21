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

}

