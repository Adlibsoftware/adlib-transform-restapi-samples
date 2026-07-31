
import fs from 'fs';
import path from 'path';
import { InputReference, OutputReference, MetadataDto } from './models';

export interface Config {
  baseUrl: string;
  apiKey: string;
  apiKeyHeader: string;
  errorCloseSeconds: number;
  pollingRateSeconds: number;
  separateJobs: boolean;
  trustCerts: boolean;

  // SubmitByReference (non-streaming) settings. When useSubmitByReference is true, the sample submits the
  // referenceInputs (UNC paths or http(s) URIs) via POST /SubmitByReference and the engine writes output directly.
  useSubmitByReference: boolean;
  referenceInputs: InputReference[];
  referenceOutput?: OutputReference;
  referenceJobMetadata: MetadataDto[];
}

// Raw shapes as stored in appsettings.json (PascalCase, matching the C# sample).
interface RawMetadata {
  Name?: string;
  Value?: string;
}

interface RawInputReference {
  Path?: string;
  Uri?: string;
  Metadata?: RawMetadata[];
}

interface RawOutputReference {
  Folder?: string;
  FileName?: string;
  Uri?: string;
}

interface RawConfig {
  BaseUrl: string;
  ApiKey: string;
  ApiKeyHeader: string;
  ErrorCloseSeconds: number;
  PollingRateSeconds: number;
  SeparateJobs: boolean;
  TrustCerts: boolean;
  UseSubmitByReference?: boolean;
  ReferenceInputs?: RawInputReference[];
  ReferenceOutput?: RawOutputReference;
  ReferenceJobMetadata?: RawMetadata[];
}

function mapMetadata(raw: RawMetadata[] | undefined): MetadataDto[] {
  if (!raw) {
    return [];
  }
  return raw.map((m) => ({ name: m.Name ?? '', value: m.Value ?? '' }));
}

function mapInputs(raw: RawInputReference[] | undefined): InputReference[] {
  if (!raw) {
    return [];
  }
  return raw.map((i) => ({ path: i.Path, uri: i.Uri, metadata: mapMetadata(i.Metadata) }));
}

function mapOutput(raw: RawOutputReference | undefined): OutputReference | undefined {
  if (!raw) {
    return undefined;
  }
  return { folder: raw.Folder, fileName: raw.FileName, uri: raw.Uri };
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
      UseSubmitByReference: false,
      ReferenceInputs: [],
      ReferenceJobMetadata: [],
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
      useSubmitByReference: raw.UseSubmitByReference ?? false,
      referenceInputs: mapInputs(raw.ReferenceInputs),
      referenceOutput: mapOutput(raw.ReferenceOutput),
      referenceJobMetadata: mapMetadata(raw.ReferenceJobMetadata),
    };
  } catch (ex) {
    console.log(`Failed to read or parse ${CONFIG_FILE}: ${(ex as Error).message}`);
    return null;
  }
}