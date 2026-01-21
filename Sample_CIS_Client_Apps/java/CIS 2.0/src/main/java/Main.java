

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static final String LOG_FILE_PATH = "log.txt";
    private static final String JOB_LOG_FILE_PATH = "JobLogs";
    private static final String INPUT_DIRECTORY = "Input";
    private static final String DOWNLOAD_DIRECTORY = "Output";
    private static Config appSettings;

    public static void main(String[] args) {
        if (!startupTasks()) {
            return;
        }

        List<String> inputFiles;
        try (Stream<Path> stream = Files.list(Paths.get(INPUT_DIRECTORY))) {
            inputFiles = stream.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            handleError("Failed to list input files: " + e.getMessage());
            return;
        }

        ApiClient client = new ApiClient(appSettings.getBaseUrl(), appSettings.getApiKeyHeader(), appSettings.getApiKey(), appSettings.isTrustCerts());

        try {
            System.out.println("SETTINGS");
            System.out.println("Url: " + appSettings.getBaseUrl());
            System.out.println("Submit as Separate Jobs: " + (appSettings.isSeparateJobs() ? "Yes" : "No"));
            System.out.println("Skip Cert Verification: " + (appSettings.isTrustCerts() ? "Yes" : "No"));
            System.out.println("Polling Rate: " + appSettings.getPollingRateSeconds() + "s\n");

            System.out.println("Press enter to start...");
            new Scanner(System.in).nextLine();


            // 1. Get Environment
            EnvironmentResponse env = client.getEnvironment();
            if (env.getRepositories().isEmpty()) {
                throw new Exception("No repositories available.");
            }
            UUID repositoryId = env.getRepositories().get(0).getId(); // Assume only one repository for simplicity
            System.out.println("Using repository: " + env.getRepositories().get(0).getName() + " (ID: " + repositoryId + ")");

            if (appSettings.isSeparateJobs() && inputFiles.size() > 1) {
                System.out.println("Submitting as multiple jobs.\n");
                // Process each file as a separate job in parallel
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (int i = 0; i < inputFiles.size(); i++) {
                    int finalI = i;
                    List<String> singleFile = List.of(inputFiles.get(i));
                    futures.add(CompletableFuture.runAsync(() -> {
                        try {
                            processJob(client, repositoryId, singleFile, finalI);
                        } catch (Exception e) {
                            handleError("Error in job " + finalI + ": " + e.getMessage());
                        }
                    }));
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            } else {
                System.out.println("Submitting as same job.\n");
                // Process all files as one job
                processJob(client, repositoryId, inputFiles, -1);
            }

            System.out.println("Demo Completed Successfully.");
        } catch (Exception e) {
            handleError("Error: " + e.getMessage());
        }

        System.out.println("Press enter to close...");
        new Scanner(System.in).nextLine();
    }

    private static void processJob(ApiClient client, UUID repositoryId, List<String> files, int id) throws Exception {
        // 2. Submit files
        String submitMessage = files.size() == 1
                ? "Submitting file: " + Paths.get(files.get(0)).getFileName() + "..."
                : "Submitting " + files.size() + " files (" + String.join(", ", files) + ") ";
        log(submitMessage, id);

        UUID jobId = client.submit(repositoryId, files);
        log("Submitted. Job ID: " + jobId + "\n", id);

        // 3. Poll Status
        JobStatusResponse status;
        do {
            Thread.sleep(appSettings.getPollingRateSeconds() * 1000L);
            status = client.getStatus(jobId);
            log("Status: " + status.getStatus() + ". ID: " + jobId, id);
        } while (!status.getStatus().startsWith("Completed"));

        if (!"CompletedSuccessful".equals(status.getStatus())) {
            throw new Exception("Job completed with status: " + status.getStatus() + ". Details: " + status.getDetails());
        }
        log("Job " + jobId + " completed successfully.\n", id);

        // 4. Download
        String location = Paths.get(DOWNLOAD_DIRECTORY, jobId.toString()).toString();
        Files.createDirectories(Paths.get(location));

        log("Downloading files from Job: " + jobId, id);
        client.download(jobId, location);
        log("Download complete. Location: " + location + "\n", id);

        // 5. Release
        log("Releasing Job: " + jobId, id);
        client.release(jobId);
        log("Job Released.\n", id);
    }

    private static boolean startupTasks() {
        try {
            Files.writeString(Paths.get(LOG_FILE_PATH), "");
        } catch (IOException e) {
            System.out.println("Failed to clear log file: " + e.getMessage());
            return false;
        }

        appSettings = Config.loadConfig();
        if (appSettings == null) {
            return false;
        }

        Path inputDir = Paths.get(INPUT_DIRECTORY);
        if (!Files.exists(inputDir)) {
            try {
                Files.createDirectory(inputDir);
            } catch (IOException e) {
                handleError("Failed to create input directory: " + e.getMessage());
                return false;
            }
        }

        Path jobLogDir = Paths.get(JOB_LOG_FILE_PATH);
        if (!Files.exists(jobLogDir)) {
            try {
                Files.createDirectory(jobLogDir);
            } catch (IOException e) {
                handleError("Failed to create job log directory: " + e.getMessage());
                return false;
            }
        } else {
            // Clear job log directory
            try (Stream<Path> stream = Files.list(jobLogDir)) {
                stream.forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ex) {
                        System.out.println("Failed to delete file in job logs: " + ex.getMessage());
                    }
                });
            } catch (IOException e) {
                System.out.println("Error clearing " + JOB_LOG_FILE_PATH + " directory: " + e.getMessage());
            }
        }

        try (Stream<Path> stream = Files.list(inputDir)) {
            if (stream.noneMatch(Files::isRegularFile)) {
                handleError("No files in Input folder to submit. Exiting.");
                return false;
            }
        } catch (IOException e) {
            handleError("Failed to check input files: " + e.getMessage());
            return false;
        }

        return true;
    }

    private static void log(String message, int id) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logMessage = timestamp + ": " + message.trim() + "\n";

        if (id == -1) {
            System.out.println(message);
            try {
                Files.writeString(Paths.get(JOB_LOG_FILE_PATH, "joblog.txt"), logMessage, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.out.println("Failed to log: " + e.getMessage());
            }
        } else {
            System.out.println("Thread: " + id + ", " + message);
            try {
                Files.writeString(Paths.get(JOB_LOG_FILE_PATH, "joblog_" + id + ".txt"), logMessage, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.out.println("Failed to log: " + e.getMessage());
            }
        }
    }

    private static void handleError(String message) {
        System.out.println(message);
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Files.writeString(Paths.get(LOG_FILE_PATH), timestamp + ": " + message + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Failed to log error: " + e.getMessage());
        }

        for (int i = appSettings.getErrorCloseSeconds(); i >= 0; i--) {
            System.out.print("\rClosing in " + i + " seconds...");
            if (i != 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
        System.exit(1);
    }
}