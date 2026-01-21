import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    private String baseUrl;
    private String apiKey;
    private String apiKeyHeader;
    private int errorCloseSeconds;
    private int pollingRateSeconds;
    private boolean separateJobs;
    private boolean trustCerts;

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

    public static Config loadConfig() {
        final String configFile = "appsettings.json";
        Path configPath = Paths.get(configFile);
        ObjectMapper mapper = new ObjectMapper();

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