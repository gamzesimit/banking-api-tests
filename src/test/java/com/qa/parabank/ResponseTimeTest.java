package com.qa.parabank;

import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.assertTrue;

/**
 * A budget per endpoint, checked on every run. This is not load testing: it is
 * one request against an idle service, which is the floor. When the floor moves
 * the cause is a change in the code, not a busy machine.
 */
public class ResponseTimeTest extends BaseApiTest {

    private static final long READ_BUDGET_MS = 1500;
    private static final long WRITE_BUDGET_MS = 3000;

    private void withinBudget(String path, long budgetMs) {
        long elapsed = given().spec(api).when().get(path).time();
        assertTrue(elapsed < budgetMs,
                path + " answered in " + elapsed + " ms, budget is " + budgetMs + " ms");
    }

    @Test(description = "Reading an account is inside its budget")
    public void accountReadIsInsideBudget() {
        withinBudget("/accounts/12345", READ_BUDGET_MS);
    }

    @Test(description = "Reading a customer is inside its budget")
    public void customerReadIsInsideBudget() {
        withinBudget("/customers/" + SEEDED_CUSTOMER_ID, READ_BUDGET_MS);
    }

    @Test(description = "Listing the accounts of a customer is inside its budget")
    public void accountListIsInsideBudget() {
        withinBudget("/customers/" + SEEDED_CUSTOMER_ID + "/accounts", READ_BUDGET_MS);
    }

    @Test(description = "Listing transactions is inside its budget")
    public void transactionListIsInsideBudget() {
        withinBudget("/accounts/12345/transactions", READ_BUDGET_MS);
    }

    @Test(description = "A transfer is inside its budget")
    public void transferIsInsideBudget() {
        long elapsed = given().spec(api)
                .queryParam("fromAccountId", 12567)
                .queryParam("toAccountId", 12789)
                .queryParam("amount", "0.01")
                .when().post("/transfer")
                .time();

        assertTrue(elapsed < WRITE_BUDGET_MS,
                "a transfer answered in " + elapsed + " ms, budget is " + WRITE_BUDGET_MS + " ms");
    }
}
