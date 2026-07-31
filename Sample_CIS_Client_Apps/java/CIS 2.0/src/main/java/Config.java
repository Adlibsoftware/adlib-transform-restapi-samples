import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Config {
    @JsonProperty("BaseUrl")
    private String baseUrl;
    @JsonProperty("ApiKey")
    private String apiKey;
    @JsonProperty("ApiKeyHeader")
    private String apiKeyHeader;
    @JsonProperty("ErrorCloseSeconds")
    private int errorCloseSeconds;
    @JsonProperty("PollingRateSeconds")
    private int pollingRateSeconds;
    @JsonProperty("SeparateJobs")
    private boolean separateJobs;
    @JsonProperty("TrustCerts")
    private boolean trustCerts;

    // SubmitByReference (non-streaming) settings. When UseSubmitByReference is true, the sample submits the
    // ReferenceInputs (UNC paths or http(s) URIs) via POST /SubmitByReference and the engine writes output directly.
    @JsonProperty("UseSubmitByReference")
    private boolean useSubmitByReference = false;
    @JsonProperty("ReferenceInputs")
    private List<InputReference> referenceInputs = new ArrayList<>();
    @JsonProperty("ReferenceOutput")
    private OutputReference referenceOutput;
    @JsonProperty("ReferenceJobMetadata")
    private List<MetadataDto> referenceJobMetadata = new ArrayList<>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeyHeader() {
        return apiKeyHeader;
    }

    public void setApiKeyHeader(String apiKeyHeader) {
        this.apiKeyHeader = apiKeyHeader;
    }

    public int getErrorCloseSeconds() {
        return errorCloseSeconds;
    }

    public void setErrorCloseSeconds(int errorCloseSeconds) {
        this.errorCloseSeconds = errorCloseSeconds;
    }

    public int getPollingRateSeconds() {
        return pollingRateSeconds;
    }

    public void setPollingRateSeconds(int pollingRateSeconds) {
        this.pollingRateSeconds = pollingRateSeconds;
    }

    public boolean isSeparateJobs() {
        return separateJobs;
    }

    public void setSeparateJobs(boolean separateJobs) {
        this.separateJobs = separateJobs;
    }

    public boolean isTrustCerts() {
        return trustCerts;
    }

    public void setTrustCerts(boolean trustCerts) {
        this.trustCerts = trustCerts;
    }

    public boolean isUseSubmitByReference() {
        return useSubmitByReference;
    }

    public void setUseSubmitByReference(boolean useSubmitByReference) {
        this.useSubmitByReference = useSubmitByReference;
    }

    public List<InputReference> getReferenceInputs() {
        return referenceInputs;
    }

    public void setReferenceInputs(List<InputReference> referenceInputs) {
        this.referenceInputs = referenceInputs;
    }

    public OutputReference getReferenceOutput() {
        return referenceOutput;
    }

    public void setReferenceOutput(OutputReference referenceOutput) {
        this.referenceOutput = referenceOutput;
    }

    public List<MetadataDto> getReferenceJobMetadata() {
        return referenceJobMetadata;
    }

    public void setReferenceJobMetadata(List<MetadataDto> referenceJobMetadata) {
        this.referenceJobMetadata = referenceJobMetadata;
    }

    public static Config loadConfig() {
        final String configFile = "appsettings.json";
        Path configPath = Paths.get(configFile);
        // Accept case-insensitive keys so PascalCase appsettings.json keys (e.g. "Path", "Folder", "FileName")
        // bind to the camelCase model fields used for the by-reference request DTOs.
        ObjectMapper mapper = new ObjectMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);

        if (!Files.exists(configPath)) {
            // Create default config
            Config defaultConfig = new Config();
            defaultConfig.setBaseUrl("https://localhost:60204");
            defaultConfig.setApiKey("your-api-key-here");
            defaultConfig.setApiKeyHeader("X-Api-Key");
            defaultConfig.setErrorCloseSeconds(5);
            defaultConfig.setPollingRateSeconds(7);
            defaultConfig.setSeparateJobs(false);
            defaultConfig.setTrustCerts(false);

            try {
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                String json = mapper.writeValueAsString(defaultConfig);
                Files.writeString(configPath, json);
                System.out.println("Created default " + configFile + " with default values.");
            } catch (IOException ex) {
                System.out.println("Failed to create " + configFile + ": " + ex.getMessage());
                return null;
            }
        }

        try {
            String json = Files.readString(configPath);
            Config config = mapper.readValue(json, Config.class);
            if (config == null || config.getBaseUrl() == null || config.getBaseUrl().isEmpty() ||
                    config.getApiKey() == null || config.getApiKey().isEmpty() ||
                    config.getApiKeyHeader() == null || config.getApiKeyHeader().isEmpty() ||
                    config.getErrorCloseSeconds() <= 0 || config.getPollingRateSeconds() <= 0) {
                System.out.println("Invalid " + configFile + ". Ensure BaseUrl, ApiKey, ApiKeyHeader, ErrorCloseSeconds, and PollingRateSeconds are valid.");
                return null;
            }
            return config;
        } catch (IOException ex) {
            System.out.println("Failed to read or parse " + configFile + ": " + ex.getMessage());
            return null;
        }
    }
}
