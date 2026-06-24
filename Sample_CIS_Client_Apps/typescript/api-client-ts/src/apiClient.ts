import axios, { AxiosInstance } from 'axios';
import https from 'https';
import FormData from 'form-data';
import fs from 'fs';
import path from 'path';
import { EnvironmentResponse, JobStatusResponse } from './models';
import contentDisposition from 'content-disposition';

export class ApiClient {
  private httpClient: AxiosInstance;
  private apiKeyHeader: string;
  private apiKey: string;
  private basePath: string;

  constructor(baseUrl: string, apiKeyHeader: string, apiKey: string, trustCerts: boolean) {
    this.apiKeyHeader = apiKeyHeader;
    this.apiKey = apiKey;
    this.basePath = baseUrl.endsWith('/')
      ? baseUrl + 'api/v2/ClientIntegration/'
      : baseUrl + '/api/v2/ClientIntegration/';

    const config: any = {};
    if (trustCerts) {
      const agent = new https.Agent({
        rejectUnauthorized: false,
      });
      config.httpsAgent = agent;
    }

    this.httpClient = axios.create(config);
  }

  async getEnvironment(): Promise<EnvironmentResponse> {
    const response = await this.httpClient.get(this.basePath + 'Environment', {
      headers: { [this.apiKeyHeader]: this.apiKey },
    });
    if (response.status < 200 || response.status > 204) {
      throw new Error(`Failed: HTTP error code: ${response.status}`);
    }
    return response.data as EnvironmentResponse;
  }

  async submit(repositoryId: string, inputFilePaths: string[]): Promise<string> {
    const form = new FormData();
    form.append('RepositoryId', repositoryId);

    for (let i = 0; i < inputFilePaths.length; i++) {
      const filePath = inputFilePaths[i];
      const fileStream = fs.createReadStream(filePath);
      form.append(`InputFiles[${i}].InputFile`, fileStream, { filename: path.basename(filePath) });
      form.append(`InputFiles[${i}].FileMetadata[0].Name`, 'TypeScript Sample App Submission');
      form.append(`InputFiles[${i}].FileMetadata[0].Value`, 'Test file uploaded via TypeScript sample app');

      //or for per file metadata mapping which needs a 1:1 ratio of list
      /*
      for (let j = 0; j < metadataList[i].length; j++) {
        form.append(`InputFiles[${i}].FileMetadata[${j}].Name`, metadataList[i][j].name);
        form.append(`InputFiles[${i}].FileMetadata[${j}].Value`, metadataList[i][j].value);
      }
      */
    }

    const response = await this.httpClient.post(this.basePath + 'Submit', form, {
      headers: { ...form.getHeaders(), [this.apiKeyHeader]: this.apiKey },
    });
    if (response.status < 200 || response.status > 204) {
      throw new Error(`Failed: HTTP error ${response.status}: ${response.statusText}`);
    }
    return response.data as string;
  }

  async getStatus(jobId: string): Promise<JobStatusResponse> {
    const response = await this.httpClient.get(this.basePath + 'Status/' + jobId, {
      headers: { [this.apiKeyHeader]: this.apiKey },
    });
    if (response.status < 200 || response.status > 204) {
      throw new Error(`Failed: HTTP error code: ${response.status}`);
    }
    return response.data as JobStatusResponse;
  }

  async download(jobId: string, downloadDirectory: string): Promise<void> {
    const response = await this.httpClient.get(this.basePath + 'Download/' + jobId, {
      headers: { [this.apiKeyHeader]: this.apiKey },
      responseType: 'stream',
    });
    if (response.status < 200 || response.status > 204) {
      throw new Error(`Failed: HTTP error code: ${response.status}`);
    }

    const fileName = this.getDownloadFileName(response.headers['content-disposition'], jobId);

    const filePath = path.join(downloadDirectory, fileName);
    const writer = fs.createWriteStream(filePath);
    response.data.pipe(writer);

    await new Promise<void>((resolve, reject) => {
      writer.on('finish', resolve);
      writer.on('error', reject);
    });
  }

  private getDownloadFileName(contentDispositionHeader: string | undefined, jobId: string): string {
    let fileName = jobId + '.unknown';

    if (contentDispositionHeader) {
      const fileNameStar = this.getContentDispositionParameter(contentDispositionHeader, 'filename*');
      let decodedFileNameStar: string | undefined;

      if (fileNameStar) {
        decodedFileNameStar = this.decodeRfc5987Value(fileNameStar);
      }

      if (decodedFileNameStar) {
        fileName = decodedFileNameStar;
      } else {
        try {
          const parsed = contentDisposition.parse(contentDispositionHeader);
          if (parsed.parameters?.filename) {
            fileName = parsed.parameters.filename;
          }
        } catch (e) {
          const errorMessage = e instanceof Error ? e.message : String(e);
          throw new Error(`Failed to parse Content-Disposition: ${errorMessage}`);
        }
      }
    }

    return path.basename(fileName);
  }

  private getContentDispositionParameter(header: string, parameterName: string): string | undefined {
    const parts = this.splitContentDispositionHeader(header);
    const lowerParameterName = parameterName.toLowerCase();

    for (const part of parts.slice(1)) {
      const equalsIndex = part.indexOf('=');
      if (equalsIndex === -1) {
        continue;
      }

      const name = part.slice(0, equalsIndex).trim().toLowerCase();
      if (name === lowerParameterName) {
        return this.unquoteHeaderValue(part.slice(equalsIndex + 1).trim());
      }
    }

    return undefined;
  }

  private splitContentDispositionHeader(header: string): string[] {
    const parts: string[] = [];
    let current = '';
    let inQuotes = false;
    let escaped = false;

    for (const char of header) {
      if (escaped) {
        current += char;
        escaped = false;
      } else if (char === '\\' && inQuotes) {
        escaped = true;
      } else if (char === '"') {
        inQuotes = !inQuotes;
        current += char;
      } else if (char === ';' && !inQuotes) {
        parts.push(current.trim());
        current = '';
      } else {
        current += char;
      }
    }

    parts.push(current.trim());
    return parts;
  }

  private unquoteHeaderValue(value: string): string {
    if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
      return value.slice(1, -1);
    }

    return value;
  }

  private decodeRfc5987Value(value: string): string | undefined {
    const firstQuote = value.indexOf("'");
    const secondQuote = value.indexOf("'", firstQuote + 1);

    if (firstQuote === -1 || secondQuote === -1) {
      return undefined;
    }

    const charset = value.slice(0, firstQuote).toLowerCase();
    const encodedValue = value.slice(secondQuote + 1);

    if (charset && charset !== 'utf-8' && charset !== 'us-ascii') {
      return undefined;
    }

    try {
      return decodeURIComponent(encodedValue);
    } catch {
      return undefined;
    }
  }

  async release(jobId: string): Promise<void> {
    const response = await this.httpClient.put(this.basePath + 'Release/' + jobId, null, {
      headers: { [this.apiKeyHeader]: this.apiKey },
    });
    if (response.status < 200 || response.status > 204) {
      throw new Error(`Failed: HTTP error code: ${response.status}`);
    }
  }
}
