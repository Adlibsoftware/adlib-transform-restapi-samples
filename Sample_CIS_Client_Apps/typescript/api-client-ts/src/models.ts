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

// A metadata key/value pair.
export interface MetadataDto {
  name: string;
  value: string;
}

// A single input file referenced by UNC/local path OR by http(s) URI (use one per entry).
// Optional per-input metadata is passed through to that input's document.
export interface InputReference {
  path?: string;
  uri?: string;
  metadata: MetadataDto[];
}

// Where the engine writes output directly: a UNC/local folder (+ optional fileName), or an http(s) uri.
export interface OutputReference {
  folder?: string;
  fileName?: string;
  uri?: string;
}

// Request body for POST /SubmitByReference. Inputs are referenced by path/URI (not uploaded) and the
// engine writes output directly to the requested destination, so no Download call is needed.
// repositoryId is optional; when omitted the API uses the first repository the key is authorized for.
export interface SubmitByReferenceRequest {
  repositoryId?: string;
  inputs: InputReference[];
  output?: OutputReference;
  metadata: MetadataDto[];
}