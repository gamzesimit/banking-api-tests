import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// A write profile. It moves one cent back and forth between two accounts so the
// pair keeps its total, which makes the script safe to run repeatedly against
// the same environment.

const BASE = __ENV.BASE_URL || 'http://localhost:8081/parabank/services/bank';
const LEFT = __ENV.LEFT_ACCOUNT || '12567';
const RIGHT = __ENV.RIGHT_ACCOUNT || '12789';

const transferLatency = new Trend('transfer_ms');
const refused = new Rate('transfers_refused');

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: __ENV.RAMP || '10s', target: Number(__ENV.VUS || 5) },
        { duration: __ENV.HOLD || '20s', target: Number(__ENV.VUS || 5) },
        { duration: '5s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1500'],
    transfers_refused: ['rate<0.01'],
  },
};

export default function () {
  const forward = __ITER % 2 === 0;
  const from = forward ? LEFT : RIGHT;
  const to = forward ? RIGHT : LEFT;

  const res = http.post(
    `${BASE}/transfer?fromAccountId=${from}&toAccountId=${to}&amount=0.01`,
    null,
    { tags: { name: 'POST /transfer' } },
  );

  transferLatency.add(res.timings.duration);
  const ok = check(res, { 'transfer accepted': (r) => r.status === 200 });
  refused.add(!ok);
}
