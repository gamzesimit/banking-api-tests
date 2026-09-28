# Load test results

Script: `load/read-endpoints.js`
Tool: k6
Profile: 10 virtual users, constant, 20 seconds, read only
Target: `GET /accounts/{id}` on a local container

## Thresholds agreed before the run

| Threshold | Value |
|---|---|
| Failed requests | under 1 per cent |
| 95th percentile response time | under 800 ms |
| Responses missing a balance field | zero |

## Result

| Measure | Value |
|---|---|
| Requests | 109,686 |
| Throughput | 5,459 requests per second |
| Failed requests | 0.00 per cent |
| Median response time | 1.23 ms |
| 95th percentile | 2.15 ms |
| Slowest single response | 107.96 ms |
| Checks passed | 219,372 of 219,372 |

All three thresholds held with a wide margin.

## How to read this

These numbers describe a single container on a developer machine with the client
on the same host, so there is no network between them. They say the application
does not fall over under a steady read load and that the response shape stays
correct while it is busy. They do not predict anything about a deployed
environment, and they are not quoted as if they did.

The one number worth keeping is the gap between the median at 1.23 ms and the
slowest response at 107.96 ms. That is the shape to watch: it usually means a
cold cache or a garbage collection pause rather than a steady slowdown, and it is
the first thing to re-measure if a threshold ever starts failing.

## Reproducing

```bash
docker run --rm --network host \
  -v "$PWD/load":/scripts \
  -e BASE_URL=http://localhost:8081/parabank/services/bank \
  -e VUS=10 -e DURATION=20s \
  grafana/k6 run /scripts/read-endpoints.js
```
