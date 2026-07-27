from pydantic import BaseModel
from datetime import datetime
from typing import List, Optional
from uuid import UUID

class ResponseStatus(BaseModel):
    success: bool = True
    message: str = ""

class GlobalVariableDto(BaseModel):
    key: str = ""
    value: str = ""

class MetadataDto(BaseModel):
    name: str = ""
    value: str = ""

class RepositoryDto(BaseModel):
    id: UUID
    name: str
    type: str
    workspaceId: UUID
    workspaceName: str

class EnvironmentResponse(ResponseStatus):
    repositories: List[RepositoryDto] = []
    globalVariables: List[GlobalVariableDto] = []
    lastChanged: Optional[datetime] = None

class JobStatusResponse(ResponseStatus):
    jobId: UUID
    repositoryId: UUID
    status: str = ""
    details: str = ""
    totalQueueTimeInSec: float = 0.0
    totalProcessingTimeInSec: float = 0.0

# A single input file referenced by UNC/local path OR by http(s) URI (use one per entry).
# Optional per-input metadata is passed through to that input's document.
class InputReference(BaseModel):
    path: Optional[str] = None
    uri: Optional[str] = None
    metadata: List[MetadataDto] = []

# Where the engine writes output directly: a UNC/local folder (+ optional fileName), or an http(s) uri.
class OutputReference(BaseModel):
    folder: Optional[str] = None
    fileName: Optional[str] = None
    uri: Optional[str] = None

# Request body for POST /SubmitByReference. Inputs are referenced by path/uri (not uploaded) and the
# engine writes output directly to the requested destination, so no Download call is needed.
# repositoryId is optional; when omitted the API uses the first repository the key is authorized for.
class SubmitByReferenceRequest(BaseModel):
    repositoryId: Optional[UUID] = None
    inputs: List[InputReference] = []
    output: Optional[OutputReference] = None
    metadata: List[MetadataDto] = []