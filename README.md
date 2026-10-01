# Banking API tests

[![tests](https://github.com/gamzesimit/banking-api-tests/actions/workflows/tests.yml/badge.svg)](https://github.com/gamzesimit/banking-api-tests/actions/workflows/tests.yml)

REST Assured and TestNG checks against the API of a retail online banking
application, with a response time budget on every endpoint. Java 17, Maven,
running on every commit.

Three defects were found at the API surface. Two of them are critical: a transfer
with a negative amount is accepted and reverses the direction of the money, and
the transfer endpoint moves money without asking who is calling.

Reports: [docs/defect-reports.md](docs/defect-reports.md)

## Running it

```bash
docker compose up -d      # ParaBank on http://localhost:8081
mvn test
```

Against another environment:

```bash
BASE_URL=https://host/parabank/services/bank mvn test
```

## What is covered

| Class | Area |
|---|---|
| `AccountsApiTest` | Response shape, two decimal places on money, unknown identifiers, ownership of every account in a list |
| `CustomersApiTest` | Response shape, no password in the payload, unknown identifiers |
| `TransferApiTest` | Amount moved to the cent, the pair keeps its total, negative amounts, unknown destination, unauthenticated caller |
| `TransactionsApiTest` | Fields a statement needs, search by amount, unknown account |
| `LoanApiTest` | A decision is returned, the down payment leaves the funding account, negative amounts, unknown customer |
| `ErrorHandlingApiTest` | Unknown paths, identifiers that are not numbers, missing amount, transfer to the same account |
| `ResponseTimeTest` | Every read endpoint and the transfer answer inside a stated time budget |

Thirty one tests. The ones that describe a rule this build breaks are marked as
known failures and carry the defect id, so the suite stays green while the
defects stay visible. Alongside each of those sits a test that pins the present
behaviour, so a fix turns into a failing test rather than passing unnoticed.

## Why these checks

Money endpoints have a property that makes them worth testing precisely: there is
one correct answer and it can be computed without knowing anything about the
implementation. A transfer either moves the stated amount or it does not. The two
accounts either hold the same total afterwards or the ledger is broken. Every
test here is built on that.

## Structure

```
src/test/java/com/qa/parabank/   test classes, BaseApiTest holds shared setup
src/test/resources/testng.xml    suite definition
docs/                            defect reports
```
