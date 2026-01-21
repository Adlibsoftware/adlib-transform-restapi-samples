import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

abstract class ResponseStatus {
    private boolean success = true;
    private String message = "";

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

class EnvironmentResponse extends ResponseStatus {
    private List<RepositoryDto> repositories = new ArrayList<>();
    private List<GlobalVariableDto> globalVariables = new ArrayList<>();
    private Instant lastChanged = null;

    public List<RepositoryDto> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<RepositoryDto> repositories) {
        this.repositories = repositories;
    }

    public List<GlobalVariableDto> getGlobalVariables() {
        return globalVariables;
    }

    public void setGlobalVariables(List<GlobalVariableDto> globalVariables) {
        this.globalVariables = globalVariables;
    }

    public Instant getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(Instant lastChanged) {
        this.lastChanged = lastChanged;
    }
}

class GlobalVariableDto {
    private String key = "";
    private String value = "";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

class MetadataDto {
    private String name = "";
    private String value = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

class JobStatusResponse extends ResponseStatus {
    private UUID jobId;
    private UUID repositoryId;
    private String status = "";
    private String details = "";
    private double totalQueueTimeInSec;
    private double totalProcessingTimeInSec;

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(UUID repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public double getTotalQueueTimeInSec() {
        return totalQueueTimeInSec;
    }

    public void setTotalQueueTimeInSec(double totalQueueTimeInSec) {
        this.totalQueueTimeInSec = totalQueueTimeInSec;
    }

    public double getTotalProcessingTimeInSec() {
        return totalProcessingTimeInSec;
    }

    public void setTotalProcessingTimeInSec(double totalProcessingTimeInSec) {
        this.totalProcessingTimeInSec = totalProcessingTimeInSec;
    }
}

class RepositoryDto {
    private UUID id;
    private String name = "";
    private String type = "";
    private UUID workspaceId;
    private String workspaceName = "";

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }
}