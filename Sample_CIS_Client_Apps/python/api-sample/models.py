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