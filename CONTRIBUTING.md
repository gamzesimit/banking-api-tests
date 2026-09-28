# Contributing

## Running the suite

```bash
docker compose up -d
mvn test
```

Against another environment:

```bash
BASE_URL=https://host/parabank/services/bank mvn test
```

## How a test is written here

1. State the rule in the test name. "A transfer moves exactly the stated amount"
   tells a reader what breaks when it goes red. "testTransfer" does not.
2. Compare money with `BigDecimal.compareTo`, never with `equals`. `200.0` and
   `200.00` are the same amount and different objects.
3. Read the balance before and after, and assert on the difference. Asserting on
   an absolute balance makes the test depend on every test that ran before it.
4. A rule this build breaks is marked `expectedExceptions = AssertionError.class`
   and carries the defect id, with a second test beside it pinning the present
   behaviour so a fix turns into a failing test.
5. Put shared setup in `BaseApiTest`. A test class that builds its own request
   specification will drift from the others.

## Reporting a defect

Add it to `docs/defect-reports.md` with the same shape as the entries there:
steps as runnable requests, result, expected, impact, and the test that covers it.
