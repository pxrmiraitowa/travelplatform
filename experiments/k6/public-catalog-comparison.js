import http from 'k6/http';
import exec from 'k6/execution';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8088').replace(/\/$/, '');
const summaryFile = __ENV.SUMMARY_FILE || 'summary.json';
const sleepSeconds = Number(__ENV.SLEEP_SECONDS || 0.05);
const endpoints = [
  { name: 'flights', path: '/api/public/flights?pageNum=1&pageSize=10' },
  { name: 'hotels', path: '/api/public/hotels?pageNum=1&pageSize=10' },
  { name: 'tours', path: '/api/public/tours?pageNum=1&pageSize=10' },
];

const metrics = {
  flights: {
    requests: new Counter('flights_requests'),
    errors: new Rate('flights_errors'),
    duration: new Trend('flights_duration', true),
  },
  hotels: {
    requests: new Counter('hotels_requests'),
    errors: new Rate('hotels_errors'),
    duration: new Trend('hotels_duration', true),
  },
  tours: {
    requests: new Counter('tours_requests'),
    errors: new Rate('tours_errors'),
    duration: new Trend('tours_duration', true),
  },
};
const businessErrors = new Rate('business_errors');

export const options = {
  maxRedirects: 0,
  tags: {
    session: __ENV.SESSION_ID || 'manual',
    run: __ENV.RUN_LABEL || 'manual',
    variant: __ENV.VARIANT || 'unspecified',
  },
  scenarios: {
    public_catalog: {
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
    flights_errors: ['rate<0.01'],
    hotels_errors: ['rate<0.01'],
    tours_errors: ['rate<0.01'],
  },
};

export default function () {
  const endpoint = endpoints[exec.scenario.iterationInTest % endpoints.length];
  const response = http.get(`${baseUrl}${endpoint.path}`, {
    tags: { endpoint: endpoint.name },
    timeout: '10s',
  });

  let applicationCode = null;
  try {
    applicationCode = response.json('code');
  } catch (_) {
    // A non-JSON response is counted as a business error below.
  }
  const succeeded = response.status === 200 && applicationCode === 200;
  metrics[endpoint.name].requests.add(1);
  metrics[endpoint.name].errors.add(!succeeded);
  metrics[endpoint.name].duration.add(response.timings.duration);
  businessErrors.add(!succeeded);

  check(response, {
    'HTTP status is 200': (result) => result.status === 200,
    'response is JSON': (result) =>
      (result.headers['Content-Type'] || '').includes('application/json'),
    'business code is 200': () => applicationCode === 200,
  }, { endpoint: endpoint.name });

  sleep(sleepSeconds);
}

export function handleSummary(data) {
  const duration = data.metrics.http_req_duration?.values || {};
  const requestRate = data.metrics.http_reqs?.values?.rate || 0;
  const errorRate = data.metrics.business_errors?.values?.rate || 0;
  const text = [
    '',
    `requests/s: ${requestRate.toFixed(2)}`,
    `business error rate: ${(errorRate * 100).toFixed(2)}%`,
    `average: ${(duration.avg || 0).toFixed(2)} ms`,
    `p95: ${(duration['p(95)'] || 0).toFixed(2)} ms`,
    '',
  ].join('\n');
  return {
    stdout: text,
    [summaryFile]: JSON.stringify(data, null, 2),
  };
}
