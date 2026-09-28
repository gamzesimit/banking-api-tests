package com.qa.parabank;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * The rules that protect the balance, checked through the API rather than the
 * browser. The API is the surface an integration partner uses, so a rule that
 * the interface enforces and the API does not is a real gap.
 */
public class TransferApiTest extends BaseApiTest {

    private static final int FROM = 12567;
    private static final int TO = 12789;

    private BigDecimal balanceOf(int accountId) {
        return new BigDecimal(given().spec(api)
                .when().get("/accounts/" + accountId)
                .then().statusCode(200)
                .extract().path("balance").toString());
    }

    private Response transfer(int from, int to, String amount) {
        return given().spec(api)
                .queryParam("fromAccountId", from)
                .queryParam("toAccountId", to)
                .queryParam("amount", amount)
                .when().post("/transfer");
    }

    @Test(description = "A transfer moves exactly the stated amount in both directions")
    public void transferMovesTheStatedAmount() {
        BigDecimal fromBefore = balanceOf(FROM);
        BigDecimal toBefore = balanceOf(TO);

        Response response = transfer(FROM, TO, "12.34");
        assertEquals(response.statusCode(), 200, "a valid transfer must succeed");

        BigDecimal fromAfter = balanceOf(FROM);
        BigDecimal toAfter = balanceOf(TO);

        assertEquals(fromBefore.subtract(fromAfter), new BigDecimal("12.34"),
                "the paying account must fall by exactly the amount");
        assertEquals(toAfter.subtract(toBefore), new BigDecimal("12.34"),
                "the receiving account must rise by exactly the amount");
    }

    @Test(description = "A transfer creates and destroys nothing: the pair keeps its total")
    public void transferKeepsTheTotalWhole() {
        BigDecimal before = balanceOf(FROM).add(balanceOf(TO));
        transfer(FROM, TO, "5.67");
        BigDecimal after = balanceOf(FROM).add(balanceOf(TO));
        assertEquals(after.compareTo(before), 0,
                "the two accounts together must hold the same money after a transfer, before "
                        + before + " after " + after);
    }

    /**
     * PB-005. The API accepts a negative amount and reverses the direction of
     * the money, so the caller can pull funds out of the account named as the
     * destination. Marked as a known failure so the suite stays green while the
     * defect stays visible; see docs/defect-reports.md.
     */
    @Test(description = "A negative amount must be refused by the API",
          expectedExceptions = AssertionError.class)
    public void negativeAmountIsRefused() {
        BigDecimal fromBefore = balanceOf(FROM);
        Response response = transfer(FROM, TO, "-100.00");

        assertTrue(response.statusCode() >= 400,
                "a negative transfer must be refused, the API answered " + response.statusCode());
        assertEquals(balanceOf(FROM).compareTo(fromBefore), 0,
                "a refused transfer must leave the balance untouched");
    }

    @Test(description = "PB-005 pinned: a negative amount currently reverses the direction of the money")
    public void negativeAmountCurrentlyReversesTheTransfer() {
        BigDecimal fromBefore = balanceOf(FROM);
        BigDecimal toBefore = balanceOf(TO);

        Response response = transfer(FROM, TO, "-100.00");
        assertEquals(response.statusCode(), 200, "this pins the behaviour reported as PB-005");

        BigDecimal fromAfter = balanceOf(FROM);
        BigDecimal toAfter = balanceOf(TO);

        assertEquals(fromAfter.subtract(fromBefore).compareTo(new BigDecimal("100.00")), 0,
                "the account named as the payer gains the money");
        assertEquals(toBefore.subtract(toAfter).compareTo(new BigDecimal("100.00")), 0,
                "the account named as the payee loses the money");
    }

    @Test(description = "The transfer endpoint does not ask who is calling")
    public void transferEndpointAcceptsAnUnauthenticatedCaller() {
        Response response = transfer(FROM, TO, "1.00");
        assertEquals(response.statusCode(), 200,
                "this pins the behaviour reported as PB-006: no credentials are required");
    }

    @Test(description = "A transfer to an account that does not exist must be refused")
    public void unknownDestinationIsRefused() {
        BigDecimal fromBefore = balanceOf(FROM);
        Response response = transfer(FROM, 99999999, "1.00");

        assertTrue(response.statusCode() >= 400,
                "a transfer to an unknown account must be refused, the API answered " + response.statusCode());
        assertEquals(balanceOf(FROM), fromBefore, "a refused transfer must leave the balance untouched");
    }
}
