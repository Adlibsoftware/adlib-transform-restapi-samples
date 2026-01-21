import fs from 'fs';
import path from 'path';
import readline from 'readline';
import { Config, loadConfig } from './config';
import { ApiClient } from './apiClient';
import { EnvironmentResponse, JobStatusResponse } from './models';

const LOG_FILE_PATH = 'log.txt';
const JOB_LOG_FILE_PATH = 'JobLogs';
const INPUT_DIRECTORY = 'Input';
const DOWNLOAD_DIRECTORY = 'Output';
let appSettings: Config | null;

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
});

function question(query: string): Promise<string> {
  return new Promise((resolve) => rl.question(query, resolve));
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function getTimestamp(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hour = String(now.getHours()).padStart(2, '0');
  const minute = String(now.getMinutes()).padStart(2, '0');
  const second = String(now.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

async function main() {
  if (!(await startupTasks())) {
    return;
  }

  let inputFiles: string[];
  try {
    inputFiles = fs
      .readdirSync(INPUT_DIRECTORY)
      .filter((file) => fs.statSync(path.join(INPUT_DIRECTORY, file)).isFile())
      .map((file) => path.join(INPUT_DIRECTORY, file));
  } catch (e) {
    handleError(`Failed to list input files: ${(e as Error).message}`);
    return;
  }

  if (!appSettings) {
    return;
  }

  const client = new ApiClient(
    appSettings.baseUrl,
    appSettings.apiKeyHeader,
    appSettings.apiKey,
    appSettings.trustCerts
  );

  try {
    console.log('SETTINGS');
    console.log(`Url: ${appSettings.baseUrl}`);
    console.log(`Submit as Separate Jobs: ${appSettings.separateJobs ? 'Yes' : 'No'}`);
    console.log(`Skip Cert Verification: ${appSettings.trustCerts ? 'Yes' : 'No'}`);
    console.log(`Polling Rate: ${appSettings.pollingRateSeconds}s\n`);

    console.log('Press enter to start...');
    await question('');

    // 1. Get Environment
    const env: EnvironmentResponse = await client.getEnvironment();
    if (env.repositories.length === 0) {
      throw new Error('No repositories available.');
    }
    const repositoryId = env.repositories[0].id; // Assume only one repository for simplicity
    console.log(`Using repository: ${env.repositories[0].name} (ID: ${repositoryId})`);

    if (appSettings.separateJobs && inputFiles.length > 1) {
      console.log('Submitting as multiple jobs.\n');
      // Process each file as a separate job in parallel
      await Promise.all(
        inputFiles.map(async (file, i) => {
          try {
            await processJob(client, repositoryId, [file], i);
          } catch (e) {
            handleError(`Error in job ${i}: ${(e as Error).message}`);
          }
        })
      );
    } else {
      console.log('Submitting as same job.\n');
      // Process all files as one job
      await processJob(client, repositoryId, inputFiles, -1);
    }

    console.log('Demo Completed Successfully.');
  } catch (e) {
    handleError(`Error: ${(e as Error).message}`);
  }

  console.log('Press enter to close...');
  await question('');
  process.exit(0);
}

async function processJob(
  client: ApiClient,
  repositoryId: string,
  files: string[],
  id: number
): Promise<void> {
  // 2. Submit files
  const submitMessage =
    files.length === 1
      ? `Submitting file: ${path.basename(files[0])}...`
      : `Submitting ${files.length} files (${files.join(', ')}) `;
  log(submitMessage, id);

  const jobId = await client.submit(repositoryId, files);
  log(`Submitted. Job ID: ${jobId}\n`, id);

  // 3. Poll Status
  let status: JobStatusResponse;
  do {
    await sleep((appSettings?.pollingRateSeconds ?? 7) * 1000);
    status = await client.getStatus(jobId);
    log(`Status: ${status.status}. ID: ${jobId}`, id);
  } while (!status.status.startsWith('Completed'));

  if (status.status !== 'CompletedSuccessful') {
    throw new Error(`Job completed with status: ${status.status}. Details: ${status.details}`);
  }
  log(`Job ${jobId} completed successfully.\n`, id);

  // 4. Download
  const location = path.join(DOWNLOAD_DIRECTORY, jobId);
  fs.mkdirSync(location, { recursive: true });

  log(`Downloading files from Job: ${jobId}`, id);
  await client.download(jobId, location);
  log(`Download complete. Location: ${location}\n`, id);

  // 5. Release
  log(`Releasing Job: ${jobId}`, id);
  await client.release(jobId);
  log(`Job Released.\n`, id);
}

async function startupTasks(): Promise<boolean> {
  try {
    fs.writeFileSync(LOG_FILE_PATH, '');
  } catch (e) {
    console.log(`Failed to clear log file: ${(e as Error).message}`);
    return false;
  }

  appSettings = loadConfig();
  if (!appSettings) {
    return false;
  }

  const inputDir = path.resolve(INPUT_DIRECTORY);
  if (!fs.existsSync(inputDir)) {
    try {
      fs.mkdirSync(inputDir);
    } catch (e) {
      handleError(`Failed to create input directory: ${(e as Error).message}`);
      return false;
    }
  }

  const jobLogDir = path.resolve(JOB_LOG_FILE_PATH);
  if (!fs.existsSync(jobLogDir)) {
    try {
      fs.mkdirSync(jobLogDir);
    } catch (e) {
      handleError(`Failed to create job log directory: ${(e as Error).message}`);
      return false;
    }
  } else {
    // Clear job log directory
    try {
      fs.readdirSync(jobLogDir).forEach((file) => {
        fs.unlinkSync(path.join(jobLogDir, file));
      });
    } catch (e) {
      console.log(`Error clearing ${JOB_LOG_FILE_PATH} directory: ${(e as Error).message}`);
    }
  }

  try {
    const hasFiles = fs.readdirSync(inputDir).some((file) => fs.statSync(path.join(inputDir, file)).isFile());
    if (!hasFiles) {
      handleError('No files in Input folder to submit. Exiting.');
      return false;
    }
  } catch (e) {
    handleError(`Failed to check input files: ${(e as Error).message}`);
    return false;
  }

  return true;
}

function log(message: string, id: number) {
  const timestamp = getTimestamp();
  const logMessage = `${timestamp}: ${message.trim()}\n`;

  if (id === -1) {
    console.log(message);
    try {
      fs.appendFileSync(path.join(JOB_LOG_FILE_PATH, 'joblog.txt'), logMessage);
    } catch (e) {
      console.log(`Failed to log: ${(e as Error).message}`);
    }
  } else {
    console.log(`Thread: ${id}, ${message}`);
    try {
      fs.appendFileSync(path.join(JOB_LOG_FILE_PATH, `joblog_${id}.txt`), logMessage);
    } catch (e) {
      console.log(`Failed to log: ${(e as Error).message}`);
    }
  }
}

async function handleError(message: string) {
  console.log(message);
  try {
    const timestamp = getTimestamp();
    fs.appendFileSync(LOG_FILE_PATH, `${timestamp}: ${message}\n`);
  } catch (e) {
    console.log(`Failed to log error: ${(e as Error).message}`);
  }

  if (!appSettings) {
    process.exit(1);
  }

  for (let i = appSettings.errorCloseSeconds; i >= 0; i--) {
    process.stdout.write(`\rClosing in ${i} seconds...`);
    if (i !== 0) {
      await sleep(1000);
    }
  }
  process.exit(1);
}

main().catch((e) => {
  handleError(`Unhandled error: ${e.message}`);
});