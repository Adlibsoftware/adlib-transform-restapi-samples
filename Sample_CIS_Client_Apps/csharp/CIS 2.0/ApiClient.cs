using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using System.Threading.Tasks;


namespace CIS_2_0
{
    public class ApiClient
    {
        private readonly HttpClient _httpClient;
        private readonly string _apiKeyHeader;
        private readonly string _apiKey;
        private readonly string _basePath;

        public ApiClient(string baseUrl, string apiKeyHeader, string apiKey, bool skipCertificateVerification = false)
        {
            if (skipCertificateVerification)
            {
                var handler = new HttpClientHandler
                {
                    ServerCertificateCustomValidationCallback = (message, cert, chain, errors) => true // Bypasses all certificate validation
                };

                _httpClient = new HttpClient(handler) { };
            }
            else
            {
                _httpClient = new HttpClient() { };
            }

            _apiKeyHeader = apiKeyHeader;
            _apiKey = apiKey;
            _basePath = baseUrl.EndsWith('/') ? $"{baseUrl}api/v2/ClientIntegration/" : $"{baseUrl}/api/v2/ClientIntegration/";
        }


        /// <summary>
        /// Adds the API key header to the HTTP request.
        /// </summary>
        /// <param name="request"></param>
        private void AddApiKeyHeader(HttpRequestMessage request)
        {
            request.Headers.Add(_apiKeyHeader, _apiKey);
        }


        /// <summary>
        /// Gets environment information for API key including repositories that are available to submit to based on your allowed
        /// workspaces and global variables.
        /// </summary>
        /// <returns></returns>
        public async Task<EnvironmentResponse> GetEnvironmentAsync()
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, $"{_basePath}Environment");
            AddApiKeyHeader(request);
            var response = await _httpClient.SendAsync(request);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadFromJsonAsync<EnvironmentResponse>();
        }


        /// <summary>
        /// Submits a job with the specified repository ID and input files.
        /// </summary>
        /// <param name="repositoryId"></param>
        /// <param name="inputFilePaths"></param>
        /// <returns></returns>
        public async Task<Guid> SubmitAsync(Guid repositoryId, List<string> inputFilePaths)
        {
            using var content = new MultipartFormDataContent();
            content.Add(new StringContent(repositoryId.ToString()), "RepositoryId");

            // Keep track of FileStreams to dispose after the request
            var fileStreams = new List<FileStream>();

            try
            {
                for (int i = 0; i < inputFilePaths.Count; i++)
                {
                    var filePath = inputFilePaths[i];
                    var fileStream = new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.Read);
                    fileStreams.Add(fileStream); // Track for later disposal

                    var fileContent = new StreamContent(fileStream);
                    fileContent.Headers.ContentType = new MediaTypeHeaderValue("application/octet-stream");
                    content.Add(fileContent, $"InputFiles[{i}].InputFile", Path.GetFileName(filePath));

                    
                    content.Add(new StringContent("C# Sample App Submission"), $"InputFiles[{i}].FileMetadata[0].Name");
                    content.Add(new StringContent("Test file uploaded via C# sample app"), $"InputFiles[{i}].FileMetadata[0].Value");

                    //below is an example on how to add per file metadata entries via 1:1 size lists (metadata and files)
                    /*
                     * metadata is not used in this sample, but here's how you would add multiple metadata entries per file
                     for(int j = 0; j < metadata[i].Count; j++) {
                        content.Add(new StringContent(content.Add(new StringContent(metadata[i][j].Name), $"InputFiles[{i}].FileMetadata[{j}].Name");
                        content.Add(new StringContent(content.Add(new StringContent(metadata[i][j].Value), $"InputFiles[{i}].FileMetadata[{j}].Value");
                     }

                     */
                }

                using var request = new HttpRequestMessage(HttpMethod.Post, $"{_basePath}Submit");
                AddApiKeyHeader(request);
                request.Content = content;
                var response = await _httpClient.SendAsync(request);
                response.EnsureSuccessStatusCode();
                return await response.Content.ReadFromJsonAsync<Guid>();
            }
            finally
            {
                // Dispose all FileStreams after the request is complete
                foreach (var fileStream in fileStreams)
                {
                    fileStream.Dispose();
                }
            }
        }


        /// <summary>
        /// Gets the status of a submitted job by its Job ID.
        /// </summary>
        /// <param name="jobId"></param>
        /// <returns></returns>
        public async Task<JobStatusResponse> GetStatusAsync(Guid jobId)
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, $"{_basePath}Status/{jobId}");
            AddApiKeyHeader(request);
            var response = await _httpClient.SendAsync(request);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadFromJsonAsync<JobStatusResponse>();
        }


        /// <summary>
        /// Downloads the result files of a completed job to the specified download directory.
        /// </summary>
        /// <param name="jobId"></param>
        /// <param name="downloadDirectory"></param>
        /// <returns></returns>
        public async Task DownloadAsync(Guid jobId, string downloadDirectory)
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, $"{_basePath}Download/{jobId}");
            AddApiKeyHeader(request);
            var response = await _httpClient.SendAsync(request);
            response.EnsureSuccessStatusCode();

            var contentDisposition = response.Content.Headers.ContentDisposition;
            var fileName = contentDisposition?.FileName.Trim('\"') ?? $"{jobId}.unknown";


            var filePath = Path.Combine(downloadDirectory, fileName);
            using var fileStream = new FileStream(filePath, FileMode.Create, FileAccess.Write, FileShare.None);
            await response.Content.CopyToAsync(fileStream);
        }


        /// <summary>
        /// Releases the job. Once released, job data is no longer available.
        /// </summary>
        /// <param name="jobId"></param>
        /// <returns></returns>
        public async Task ReleaseAsync(Guid jobId)
        {
            using var request = new HttpRequestMessage(HttpMethod.Put, $"{_basePath}Release/{jobId}");
            AddApiKeyHeader(request);
            var response = await _httpClient.SendAsync(request);
            response.EnsureSuccessStatusCode();
        }
    }
}
