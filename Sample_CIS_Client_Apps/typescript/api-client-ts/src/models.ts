export interface ResponseStatus {
  success: boolean;
  message: string;
}

export interface EnvironmentResponse extends ResponseStatus {
  repositories: RepositoryDto[];
  globalVariables: GlobalVariableDto[];
  lastChanged: Date | null;
}

export interface GlobalVariableDto {
  key: string;
  value: string;
}

export interface JobStatusResponse extends ResponseStatus {
  jobId: string;
  repositoryId: string;
  status: string;
  details: string;
  totalQueueTimeInSec: number;
  totalProcessingTimeInSec: number;
}

export interface RepositoryDto {
  id: string;
  name: string;
  type: string;
  workspaceId: string;
  workspaceName: string;
}