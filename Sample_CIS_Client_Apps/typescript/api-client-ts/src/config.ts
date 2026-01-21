
import fs from 'fs';
import path from 'path';

export interface Config {
  baseUrl: string;
  apiKey: string;
  apiKeyHeader: string;
  errorCloseSeconds: number;
  pollingRateSeconds: number;
  separateJobs: boolean;
  trustCerts: boolean;
}

const CONFIG_FILE = 'appsettings.json';

export function loadConfig(): Config | null {
  const configPath = path.resolve(CONFIG_FILE);

  if (!fs.existsSync(configPath)) {
    // Create default config
    const defaultConfig: Config = {
      baseUrl: 'https://localhost:60204',
      apiKey: 'your-api-key-here',
      apiKeyHeader: 'X-Api-Key',
      errorCloseSeconds: 5,
      pollingRateSeconds: 7,
      separateJobs: false,
      trustCerts: false,
    };

    try {
      fs.writeFileSync(configPath, JSON.stringify(defaultConfig, null, 2));
      console.log(`Created default ${CONFIG_FILE} with default values.`);
    } catch (ex) {
      console.log(`Failed to create ${CONFIG_FILE}: ${(ex as Error).message}`);
      return null;
    }
  }

  try {
    const json = fs.readFileSync(configPath, 'utf-8');
    const config: Config = JSON.parse(json);
    if (
      !config ||
      !config.baseUrl ||
      !config.apiKey ||
      !config.apiKeyHeader ||
      config.errorCloseSeconds <= 0 ||
      config.pollingRateSeconds <= 0
    ) {
      console.log(
        `Invalid ${CONFIG_FILE}. Ensure BaseUrl, ApiKey, ApiKeyHeader, ErrorCloseSeconds, and PollingRate are valid.`
      );
      return null;
    }
    return config;
  } catch (ex) {
    console.log(`Failed to read or parse ${CONFIG_FILE}: ${(ex as Error).message}`);
    return null;
  }
}