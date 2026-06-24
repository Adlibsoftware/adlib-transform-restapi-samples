
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

interface RawConfig {
  BaseUrl: string;
  ApiKey: string;
  ApiKeyHeader: string;
  ErrorCloseSeconds: number;
  PollingRateSeconds: number;
  SeparateJobs: boolean;
  TrustCerts: boolean;
}

const CONFIG_FILE = 'appsettings.json';

export function loadConfig(): Config | null {
  const configPath = path.resolve(CONFIG_FILE);

  if (!fs.existsSync(configPath)) {
    const defaultRaw: RawConfig = {
      BaseUrl: 'https://localhost:60204',
      ApiKey: 'your-api-key-here',
      ApiKeyHeader: 'X-Api-Key',
      ErrorCloseSeconds: 5,
      PollingRateSeconds: 7,
      SeparateJobs: false,
      TrustCerts: false,
    };

    try {
      fs.writeFileSync(configPath, JSON.stringify(defaultRaw, null, 2));
      console.log(`Created default ${CONFIG_FILE} with default values.`);
    } catch (ex) {
      console.log(`Failed to create ${CONFIG_FILE}: ${(ex as Error).message}`);
      return null;
    }
  }

  try {
    const json = fs.readFileSync(configPath, 'utf-8');
    const raw: RawConfig = JSON.parse(json);
    if (
      !raw ||
      !raw.BaseUrl ||
      !raw.ApiKey ||
      !raw.ApiKeyHeader ||
      raw.ErrorCloseSeconds <= 0 ||
      raw.PollingRateSeconds <= 0
    ) {
      console.log(
        `Invalid ${CONFIG_FILE}. Ensure BaseUrl, ApiKey, ApiKeyHeader, ErrorCloseSeconds, and PollingRate are valid.`
      );
      return null;
    }
    return {
      baseUrl: raw.BaseUrl,
      apiKey: raw.ApiKey,
      apiKeyHeader: raw.ApiKeyHeader,
      errorCloseSeconds: raw.ErrorCloseSeconds,
      pollingRateSeconds: raw.PollingRateSeconds,
      separateJobs: raw.SeparateJobs,
      trustCerts: raw.TrustCerts,
    };
  } catch (ex) {
    console.log(`Failed to read or parse ${CONFIG_FILE}: ${(ex as Error).message}`);
    return null;
  }
}