package com.qa.parabank;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * The loan endpoint decides whether to lend and, when it approves, opens an
 * account and takes a down payment. Both halves have to hold together: an
 * approval that does not move the down payment is a free loan.
 */
public class LoanApiTest extends BaseApiTest {

    private static final int FUNDING_ACCOUNT = 12456;

    private BigDecimal balanceOf(int accountId) {
        return new BigDecimal(given().spec(api)
                .when().get("/accounts/" + accountId)
                .then().statusCode(200)
                .extract().path("balance").toString());
    }

    private Response requestLoan(String amount, String downPayment) {
        return given().spec(api)
                .queryParam("customerId", SEEDED_CUSTOMER_ID)
                .queryParam("amount", amount)
                .queryParam("downPayment", downPayment)
                .queryParam("fromAccountId", FUNDING_ACCOUNT)
                .when().post("/requestLoan");
    }

    @Test(description = "A loan request is answered with a decision, not an empty body")
    public void loanRequestReturnsADecision() {
        Response response = requestLoan("1000", "100");
        assertEquals(response.statusCode(), 200, "the endpoint must answer a well formed request");
        String approved = response.jsonPath().getString("approved");
        assertNotNull(approved, "the answer must state whether the loan was approved: " + response.asString());
    }

    @Test(description = "An approved loan takes the down payment out of the funding account")
    public void approvedLoanTakesTheDownPayment() {
        BigDecimal before = balanceOf(FUNDING_ACCOUNT);
        Response response = requestLoan("500", "50");
        boolean approved = Boolean.TRUE.equals(response.jsonPath().getBoolean("approved"));

        if (!approved) {
            throw new org.testng.SkipException("the application declined this loan, nothing to verify");
        }

        BigDecimal after = balanceOf(FUNDING_ACCOUNT);
        assertEquals(before.subtract(after).compareTo(new BigDecimal("50")), 0,
                "the down payment must leave the funding account exactly once");
    }

    @Test(description = "A negative loan amount must be refused")
    public void negativeLoanAmountIsRefused() {
        Response response = requestLoan("-1000", "100");
        boolean approved = Boolean.TRUE.equals(response.jsonPath().getBoolean("approved"));
        assertFalse(approved, "a negative loan must never be approved");
    }

    @Test(description = "A loan for a customer that does not exist must be refused")
    public void unknownCustomerIsRefused() {
        Response response = given().spec(api)
                .queryParam("customerId", 99999999)
                .queryParam("amount", "1000")
                .queryParam("downPayment", "100")
                .queryParam("fromAccountId", FUNDING_ACCOUNT)
                .when().post("/requestLoan");

        boolean approved = Boolean.TRUE.equals(response.jsonPath().getBoolean("approved"));
        assertFalse(approved, "a loan must not be approved for a customer that does not exist");
    }
}
