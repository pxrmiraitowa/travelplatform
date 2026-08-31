import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8088').replace(/\/$/, '');
const targetPath = __ENV.TARGET_PATH || '/api/public/flights';
const summaryFile = __ENV.SUMMARY_FILE || 'experiments/results/latest-summary.json';
const businessErrors = new Rate('business_errors');

export const options = {
  maxRedirects: 0,
  tags: {
    session: __ENV.SESSION_ID || 'manual',
    run: __ENV.RUN_LABEL || 'manual',
    variant: __ENV.VARIANT || 'unspecified',
  },
  scenarios: {
    public_read: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 30),
      duration: __ENV.DURATION || '60s',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    business_errors: ['rate<0.01'],
    http_req_duration: ['p(95)<1500'],
  },
};

export default function () {
  const response = http.get(`${baseUrl}${targetPath}`, {
    tags: { endpoint: targetPath },
    timeout: '10s',
  });

  let applicationCode = null;
  try {
    applicationCode = response.json('code');
  } catch (_) {
    // Non-JSON responses are counted as business failures below.
  }

  const businessSucceeded = response.status === 200 && applicationCode === 200;
  businessErrors.add(!businessSucceeded);

  check(response, {
    'status is 200': (result) => result.status === 200,
    'response is JSON': (result) =>
      (result.headers['Content-Type'] || '').includes('application/json'),
    'application code is 200': () => applicationCode === 200,
  });

  sleep(Number(__ENV.SLEEP_SECONDS || 0.1));
}

export function handleSummary(data) {
  const requests = data.metrics.http_reqs?.values?.count ?? 0;
  const httpFailedRate = data.metrics.http_req_failed?.values?.rate ?? 0;
  const businessFailedRate = data.metrics.business_errors?.values?.rate ?? 0;
  const duration = data.metrics.http_req_duration?.values ?? {};
  const summary = [
    '',
    `requests: ${requests}`,
    `HTTP error rate: ${(httpFailedRate * 100).toFixed(2)}%`,
    `business error rate: ${(businessFailedRate * 100).toFixed(2)}%`,
    `avg: ${(duration.avg ?? 0).toFixed(2)} ms`,
    `p95: ${(duration['p(95)'] ?? 0).toFixed(2)} ms`,
    '',
  ].join('\n');

  return {
    stdout: summary,
    [summaryFile]: JSON.stringify(data, null, 2),
  };
}
