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

---

# Write profile

Script: `load/transfer-write-load.js`
Profile: ramp to 5 virtual users over 10 seconds, hold 20 seconds, ramp down
Target: `POST /transfer` on a local container

The script moves one cent from the left account to the right and back again on
alternate iterations, so the pair holds the same total at the end as at the
start. That is what makes it safe to run against the same environment as often
as needed.

## Thresholds agreed before the run

| Threshold | Value |
|---|---|
| Failed requests | under 1 per cent |
| 95th percentile response time | under 1500 ms |
| Refused transfers | under 1 per cent |

## Result

| Measure | Value |
|---|---|
| Transfers | 15,137 |
| Refused | 0.00 per cent |
| Failed requests | 0.00 per cent |
| Median response time | 8.17 ms |
| 90th percentile | 15.50 ms |
| 95th percentile | 18.45 ms |

All three thresholds held.

## One number that is not a measurement

The run recorded a maximum response time of over fifteen minutes. That is not
the application. The container running the load generator was suspended part way
through by the host, and the request that was in flight carried the pause in its
timing. It is written down here rather than quietly dropped, because a single
enormous outlier is exactly the shape a real stall would take, and the only way
to tell them apart is to know what the machine was doing.

The figures that stand are the median and the percentiles, which are computed
over fifteen thousand requests and are not moved by one outlier.

## Reproducing

```bash
docker run --rm --network host \
  -v "$PWD/load":/scripts \
  -e BASE_URL=http://localhost:8081/parabank/services/bank \
  -e VUS=5 \
  grafana/k6 run /scripts/transfer-write-load.js
```
