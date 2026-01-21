using System.Net.NetworkInformation;
using System.Text.Json;
using CIS_2_0;


/// <summary>
/// Check ApiClient.cs, Models.cs and Main function here for API usage examples.
/// </summary>
class Program
{
    private static string LogFilePath = "log.txt";
    private static string JobLogFilePath = "JobLogs";
    private static string InputDirectory = "Input";
    private static string DownloadDirectory = "Output";
    private static Config _appSettings;

    static async Task Main(string[] args)
    {

        if (!StartupTasks())
        {
            return;
        }

        var inputFiles = Directory.GetFiles(InputDirectory);
        var client = new ApiClient(_appSettings.BaseUrl, _appSettings.ApiKeyHeader, _appSettings.ApiKey, _appSettings.TrustCerts);

        try
        {
            Console.WriteLine($"SETTINGS");
            Console.WriteLine($"Url: {_appSettings.BaseUrl}");
            Console.WriteLine($"Submit as Separate Jobs: {(_appSettings.SeparateJobs ? "Yes" : "No")}");
            Console.WriteLine($"Skip Cert Verification: {(_appSettings.TrustCerts ? "Yes" : "No")}");
            Console.WriteLine($"Polling Rate: {_appSettings.PollingRateSeconds}s");


            Console.WriteLine("Press enter to start...");
            Console.ReadLine();






            // 1. Get Environment
            var env = await client.GetEnvironmentAsync();
            if (env.repositories.Count == 0)
            {
                throw new Exception("No repositories available.");
            }
            var repositoryId = env.repositories.First().Id; // Assume only one repository for simplicity
            Console.WriteLine($"Using repository: {env.repositories.First().Name} (ID: {repositoryId})");



            if (_appSettings.SeparateJobs && inputFiles.Length > 1)
            {
                Console.WriteLine("Submitting as multiple jobs.\n");
                // Process each file as a separate job in parallel
                var tasks = new List<Task>();
                foreach (var file in inputFiles)
                {
                    tasks.Add(ProcessJobAsync(client, repositoryId, new List<string> { file }, tasks.Count));
                }
                await Task.WhenAll(tasks);
            }
            else
            {
                Console.WriteLine("Submitting as same job.\n");
                // Process all files as one job
                await ProcessJobAsync(client, repositoryId, new List<string>(inputFiles), -1);
            }


            Console.WriteLine("Demo Completed Successfully.");
        }
        catch (HttpRequestException ex) when (ex.StatusCode.HasValue && ((int)ex.StatusCode.Value >= 400))
        {
            HandleError($"API error: {ex.Message}. Status: {ex.StatusCode}");
        }
        catch (Exception ex)
        {
            HandleError($"Error: {ex.Message}");
        }

        Console.WriteLine("Press enter to close...");
        Console.ReadLine();
    }


    /// <summary>
    /// Processes a job: submits files, polls status, downloads results, and releases the job.
    /// </summary>
    /// <param name="client">The API client to connect to endpoints</param>
    /// <param name="repositoryId">The repository Id we are will be sumbitting to</param>
    /// <param name="files">The files for submission</param>
    /// <param name="id">Thread ID if we are submitting multiple jobs</param>
    /// <returns></returns>
    /// <exception cref="Exception"></exception>
    private static async Task ProcessJobAsync(ApiClient client, Guid repositoryId, List<string> files, int id = -1)
    {
        // 2. Submit files
        string submitMessage = files.Count == 1
            ? $"Submitting file: {Path.GetFileName(files.First())}..."
            : $"Submitting {files.Count} files ({string.Join(", ", files)}) ";
        Log(submitMessage, id);

        var jobId = await client.SubmitAsync(repositoryId, files);
        Log($"Submitted. Job ID: {jobId}\n", id);

        // 3. Poll Status
        JobStatusResponse status;
        do
        {
            await Task.Delay(_appSettings.PollingRateSeconds * 1000);
            status = await client.GetStatusAsync(jobId);
            Log($"Status: {status.Status}. ID: {jobId}", id);
        } while (!status.Status.StartsWith("Completed"));

        if (status.Status != "CompletedSuccessful")
        {
            throw new Exception($"Job completed with status: {status.Status}. Details: {status.Details}");
        }
        Log($"Job {jobId} completed successfully.\n", id);

        // 4. Download
        string location = Path.Combine(DownloadDirectory, jobId.ToString());
        Directory.CreateDirectory(location); // Ensure the directory exists

        Log($"Downloading files from Job: {jobId}", id);
        await client.DownloadAsync(jobId, location);
        Log($"Download complete. Location: {location}\n", id);

        // 5. Release
        Log($"Releasing Job: {jobId}", id);
        await client.ReleaseAsync(jobId);
        Log("Job Released.\n", id);
    }


    /// <summary>
    /// Performs startup tasks. Clears log, loads config, ensures directories exist, and checks for input files.
    /// </summary>
    /// <returns></returns>
    private static bool StartupTasks()
    {
        File.WriteAllText(LogFilePath, string.Empty);

        if (!Config.LoadConfig(ref _appSettings))
        {
            return false;
        }


        if (!Directory.Exists(InputDirectory))
        {
            Directory.CreateDirectory(InputDirectory);
        }


        if (!Directory.Exists(JobLogFilePath))
        {
            Directory.CreateDirectory(JobLogFilePath);
        }
        else //clear job log directory
        {
            try
            {
                Array.ForEach(Directory.GetFiles(JobLogFilePath), File.Delete);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error clearing {JobLogFilePath} directory: {ex.Message}");
            }
        }

        if (Directory.GetFiles(InputDirectory).Length == 0)
        {
            HandleError("No files in Input folder to submit. Exiting.");
            return false;
        }

        return true;
    }


    /// <summary>
    /// Logs messages to console and to a log file. If id is -1, logs to a general log file; otherwise, logs to a thread-specific log file.
    /// </summary>
    /// <param name="message"></param>
    /// <param name="id"></param>
    private static void Log(string message, int id)
    {
        if (id == -1)
        {
            Console.WriteLine(message);
            File.AppendAllText(Path.Combine(JobLogFilePath, $"joblog.txt"), $"{DateTime.Now}: {message.TrimEnd('\n')}{Environment.NewLine}");
        }
        else
        {

            Console.WriteLine($"Thread: {id}, {message}");
            File.AppendAllText(Path.Combine(JobLogFilePath, $"joblog_{id}.txt"), $"{DateTime.Now}: {message.TrimEnd('\n')}{Environment.NewLine}");
        }
    }


    /// <summary>
    /// Hnadles errors by logging the message, waiting for a specified time, and then exiting the application.
    /// </summary>
    /// <param name="message"></param>
    private static void HandleError(string message)
    {
        Console.WriteLine(message);
        File.AppendAllText(LogFilePath, $"{DateTime.Now}: {message}{Environment.NewLine}");

        for (int i = _appSettings.ErrorCloseSeconds; i >= 0; i--)
        {
            Console.Write($"\rClosing in {i} seconds...");
            if (i != 0) Thread.Sleep(1000);
        }

        Environment.Exit(1);
    }
}
