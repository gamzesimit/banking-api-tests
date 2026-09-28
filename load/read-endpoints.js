import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// A read only load profile. Nothing here moves money, so the script is safe to
// run repeatedly against the same environment.

const BASE = __ENV.BASE_URL || 'http://localhost:8081/parabank/services/bank';

const accountLatency = new Trend('account_lookup_ms');
const wrongShape = new Rate('responses_missing_balance');

export const options = {
  scenarios: {
    steady: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 10),
      duration: __ENV.DURATION || '30s',
    },
  },
  thresholds: {
    // the numbers a team would agree before running, not after
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    responses_missing_balance: ['rate==0'],
  },
};

const accounts = [12345, 12456, 12567, 12678, 12789];

export default function () {
  const id = accounts[Math.floor(Math.random() * accounts.length)];
  const res = http.get(`${BASE}/accounts/${id}`, {
    headers: { Accept: 'application/json' },
    tags: { name: 'GET /accounts/{id}' },
  });

  accountLatency.add(res.timings.duration);

  const ok = check(res, {
    'status is 200': (r) => r.status === 200,
    'body carries a balance': (r) => {
      try { return r.json('balance') !== undefined; } catch { return false; }
    },
  });

  wrongShape.add(!ok);
}
