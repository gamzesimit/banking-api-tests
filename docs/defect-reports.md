# Defect reports

Application under test: ParaBank, `parasoft/parabank:latest`, run locally.
Surface under test: the REST API at `/parabank/services/bank`.
Tested September 2026.

ParaBank is published as a practice target and is known to carry deliberate
faults. The reports below are written the way they would be written against a
real product, because the point of the exercise is the reasoning, not the
novelty of the finding.

---

## PB-005 — A negative amount reverses the direction of a transfer

**Severity:** Critical
**Endpoint:** `POST /transfer?fromAccountId={a}&toAccountId={b}&amount={n}`
**Status:** Reproducible on every attempt

**Steps**
```
GET  /accounts/12567   -> balance 181.99
GET  /accounts/12789   -> balance  18.01
POST /transfer?fromAccountId=12567&toAccountId=12789&amount=-500
GET  /accounts/12567   -> balance 681.99
GET  /accounts/12789   -> balance -481.99
```

**Result**
The call answers `200`. Account 12567, named as the payer, gains 500.00.
Account 12789, named as the payee, loses 500.00 and is driven negative.

**Expected**
The endpoint rejects any amount at or below zero with a `400` and moves nothing.
Direction is not something a caller should be able to invert by changing a sign.

**Impact**
A caller can pull money out of an account by naming it as the destination. The
account that loses the money is never asked. Combined with PB-006 this needs no
credentials at all.

**Covered by** `TransferApiTest.negativeAmountIsRefused` (marked as a known
failure) and `TransferApiTest.negativeAmountCurrentlyReversesTheTransfer`, which
pins the present behaviour so a fix shows up as a failing test.

---

## PB-006 — The transfer endpoint does not ask who is calling

**Severity:** Critical
**Endpoint:** `POST /transfer`
**Status:** Reproducible on every attempt

**Steps**
```
curl -X POST "http://localhost:8081/parabank/services/bank/transfer?fromAccountId=12567&toAccountId=12789&amount=1"
```
No session, no token, no header of any kind.

**Result**
`200`, and the money moves.

The read endpoints behave the same way: `GET /accounts/{id}` and
`GET /customers/{id}` return account balances and customer details to any caller.

**Expected**
Every endpoint that reads or moves money requires an authenticated caller, and
authorises that caller against the account named in the request.

**Impact**
Account numbers are sequential in this data set, so a caller can walk the range
and read every balance, then move money between any two of them.

**Covered by** `TransferApiTest.transferEndpointAcceptsAnUnauthenticatedCaller`.

---

## PB-007 — An unknown account is answered as if it existed

**Severity:** Medium
**Endpoint:** `GET /accounts/{id}`, `GET /customers/{id}`
**Status:** Reproducible

**Result**
Requesting an identifier that does not exist does not answer `404`. A caller
cannot tell "this account has no balance" apart from "this account is not there",
which pushes the decision into every client that integrates with the API.

**Expected**
`404` with a body that names what was not found.

**Covered by** `AccountsApiTest.unknownAccountIsNotReportedAsSuccess` and
`CustomersApiTest.unknownCustomerIsNotReportedAsSuccess`.

---

## Rules that hold

Recorded because a report that only lists faults is not a test result.

- A valid transfer moves exactly the stated amount, to the cent.
- The two accounts together hold the same money after a transfer, so nothing is
  created or destroyed.
- A transfer to an account that does not exist is refused with `400` and leaves
  the paying balance untouched.
- Balances are returned with two decimal places rather than as rounded figures.
- A customer record does not carry a password field.
- An unknown path, a non numeric identifier and a transfer with no amount are
  each refused rather than answered with a success code.
- A transfer from an account to itself leaves the balance where it was.
