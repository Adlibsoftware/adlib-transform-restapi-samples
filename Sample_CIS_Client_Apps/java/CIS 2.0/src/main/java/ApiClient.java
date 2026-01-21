import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


public class ApiClient {
    private final CloseableHttpClient httpClient;
    private final String apiKeyHeader;
    private final String apiKey;
    private final String basePath;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public ApiClient(String baseUrl, String apiKeyHeader, String apiKey, boolean trustCerts) {
        this.apiKeyHeader = apiKeyHeader;
        this.apiKey = apiKey;
        this.basePath = baseUrl.endsWith("/") ? baseUrl + "api/v2/ClientIntegration/" : baseUrl + "/api/v2/ClientIntegration/";

        var connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create();

        if (trustCerts) {
            // Bypass SSL certificate validation and hostname verification
            SSLContext sslContext;
            try {
                sslContext = SSLContextBuilder.create()
                        .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                        .build();
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                throw new RuntimeException("Failed to create SSL context", e);
            }

            var tlsStrategy = new DefaultClientTlsStrategy(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE);

            connectionManagerBuilder.setTlsSocketStrategy(tlsStrategy);
        }

        var connectionManager = connectionManagerBuilder.build();

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    public EnvironmentResponse getEnvironment() throws IOException, ParseException {
        HttpGet get = new HttpGet(basePath + "Environment");
        get.addHeader(apiKeyHeader, apiKey);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            if (response.getCode() < 200 || response.getCode() > 204) {
                throw new IOException("Failed : HTTP error code : " + response.getCode());
            }
            String body = EntityUtils.toString(response.getEntity());
            return objectMapper.readValue(body, EnvironmentResponse.class);
        }
    }

    public UUID submit(UUID repositoryId, List<String> inputFilePaths) throws IOException, ParseException {
        HttpPost post = new HttpPost(basePath + "Submit");
        post.addHeader(apiKeyHeader, apiKey);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("RepositoryId", repositoryId.toString());

        // Use InputStreams so the multipart entity can stream the file content
        // Keep references to streams so we can close them after the request completes
        List<java.io.InputStream> streams = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < inputFilePaths.size(); i++) {
                Path path = Paths.get(inputFilePaths.get(i));
                String fileName = path.getFileName().toString();
                java.io.InputStream is = Files.newInputStream(path);
                streams.add(is);
                builder.addBinaryBody("InputFiles[" + i + "].InputFile", is, ContentType.APPLICATION_OCTET_STREAM, fileName);
                builder.addTextBody("InputFiles[" + i + "].FileMetadata[0].Name", "Java Sample App Submission");
                builder.addTextBody("InputFiles[" + i + "].FileMetadata[0].Value", "Test file uploaded via Java sample app");

                // or if you want to add more metadata, repeat the above two lines with different indices when 1:1 size ratio of lists
                /*
                List<List<Metadata>> m = new ArrayList<>();
                m.add(new ArrayList<Metadata>(new Metadata("Name1", "Value1")));

                for (int j = 0; j < m.get(i).size(); j++) {
                    builder.addTextBody("InputFiles[" + i + "].FileMetadata[" + j + "].Name", m.get(i).get(j).getName());
                    builder.addTextBody("InputFiles[" + i + "].FileMetadata[" + j + "].Value", m.get(i).get(j).getValue());
                }
                */
            }

            HttpEntity entity = builder.build();
            post.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                if (response.getCode() < 200 || response.getCode() > 204) {
                    throw new IOException("Failed : HTTP error code : " + response.getCode());
                }
                String body = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(body, UUID.class);
            }
        } finally {
            // Ensure all streams are closed to avoid resource leaks
            for (java.io.InputStream s : streams) {
                try {
                    if (s != null) s.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public JobStatusResponse getStatus(UUID jobId) throws IOException, ParseException {
        HttpGet get = new HttpGet(basePath + "Status/" + jobId);
        get.addHeader(apiKeyHeader, apiKey);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            if (response.getCode() < 200 || response.getCode() > 204) {
                throw new IOException("Failed : HTTP error code : " + response.getCode());
            }
            String body = EntityUtils.toString(response.getEntity());
            return objectMapper.readValue(body, JobStatusResponse.class);
        }
    }

    public void download(UUID jobId, String downloadDirectory) throws IOException {
        HttpGet get = new HttpGet(basePath + "Download/" + jobId);
        get.addHeader(apiKeyHeader, apiKey);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            if (response.getCode() < 200 || response.getCode() > 204) {
                throw new IOException("Failed : HTTP error code : " + response.getCode());
            }

            String fileName = jobId + ".unknown";
            Header cdHeader = response.getFirstHeader("Content-Disposition");
            String contentDisposition = cdHeader != null ? cdHeader.getValue() : null;
            if (contentDisposition != null) {
                String[] parts = contentDisposition.split(";");
                for (String part : parts) {
                    if (part.trim().startsWith("filename=")) {
                        fileName = part.trim().substring("filename=".length()).replace("\"", "");
                        break;
                    }
                }
            }

            Path filePath = Paths.get(downloadDirectory, fileName);
            try (InputStream is = response.getEntity().getContent()) {
                Files.copy(is, filePath);
            }
        }
    }

    public void release(UUID jobId) throws IOException {
        HttpPut put = new HttpPut(basePath + "Release/" + jobId);
        put.addHeader(apiKeyHeader, apiKey);

        try (CloseableHttpResponse response = httpClient.execute(put)) {
            if (response.getCode() < 200 || response.getCode() > 204) {
                throw new IOException("Failed : HTTP error code : " + response.getCode());
            }
        }
    }
}