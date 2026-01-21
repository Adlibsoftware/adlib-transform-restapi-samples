using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace CIS_2_0
{
    public class Config
    {
        public string BaseUrl { get; set; }
        public string ApiKey { get; set; }
        public string ApiKeyHeader { get; set; }
        public int ErrorCloseSeconds { get; set; }
        public int PollingRateSeconds { get; set; }

        public bool SeparateJobs { get; set; } = false;
        public bool TrustCerts { get; set; } = false;


        public static bool LoadConfig(ref Config _config)
        {
            const string configFile = "appsettings.json";
            if (!File.Exists(configFile))
            {
                // Create default config
                var defaultConfig = new Config
                {
                    BaseUrl = "https://localhost:60204",
                    ApiKey = "your-api-key-here",
                    ApiKeyHeader = "X-Api-Key",
                    ErrorCloseSeconds = 5,
                    PollingRateSeconds = 7,
                    SeparateJobs = false,
                    TrustCerts = false
                };

                try
                {
                    File.WriteAllText(configFile, JsonSerializer.Serialize(defaultConfig, new JsonSerializerOptions { WriteIndented = true }));
                    Console.WriteLine($"Created default {configFile} with default values.");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Failed to create {configFile}: {ex.Message}");
                    return false;
                }
            }

            try
            {
                var json = File.ReadAllText(configFile);
                _config = JsonSerializer.Deserialize<Config>(json);
                if (_config == null || string.IsNullOrEmpty(_config.BaseUrl) || string.IsNullOrEmpty(_config.ApiKey) ||
                    string.IsNullOrEmpty(_config.ApiKeyHeader) || _config.ErrorCloseSeconds <= 0 || _config.PollingRateSeconds <= 0)
                {
                    Console.WriteLine("$Invalid {configFile}. Ensure BaseUrl, ApiKey, ApiKeyHeader, ErrorCloseSeconds, and PollingRate are valid.");
                    return false;
                }
                return true;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Failed to read or parse {configFile}: {ex.Message}");
                return false;
            }

        }
    }
}
