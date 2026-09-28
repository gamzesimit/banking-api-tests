package com.qa.parabank;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;

/**
 * A transaction list is the record a customer disputes a charge from, so the
 * fields on it have to be complete and the searches over it have to agree with
 * the list itself.
 */
public class TransactionsApiTest extends BaseApiTest {

    private static final int ACCOUNT = 12345;

    @Test(description = "Every transaction carries the fields a statement needs")
    public void transactionCarriesTheExpectedShape() {
        List<Map<String, Object>> rows = given().spec(api)
                .when().get("/accounts/" + ACCOUNT + "/transactions")
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        assertFalse(rows.isEmpty(), "the seeded account must carry at least one transaction");
        for (Map<String, Object> row : rows) {
            assertNotNull(row.get("id"), "a transaction needs an identifier");
            assertNotNull(row.get("date"), "a transaction needs a date");
            assertNotNull(row.get("amount"), "a transaction needs an amount");
            assertNotNull(row.get("type"), "a transaction needs a type");
            assertNotNull(row.get("description"), "a transaction needs a description");
            assertEquals(row.get("accountId"), ACCOUNT, "a transaction must belong to the account it was read from");
        }
    }

    @Test(description = "Searching by an amount returns only transactions with that amount")
    public void searchByAmountReturnsOnlyThatAmount() {
        List<Map<String, Object>> all = given().spec(api)
                .when().get("/accounts/" + ACCOUNT + "/transactions")
                .then().statusCode(200)
                .extract().jsonPath().getList("$");

        Object amount = all.get(0).get("amount");

        given().spec(api)
        .when()
            .get("/accounts/" + ACCOUNT + "/transactions/amount/" + amount)
        .then()
            .statusCode(200)
            .body("amount", everyItem(equalTo(amount)));
    }

    @Test(description = "Searching by an amount that does not occur returns an empty list, not an error")
    public void searchByAnAmountThatDoesNotOccur() {
        Response response = given().spec(api)
                .when().get("/accounts/" + ACCOUNT + "/transactions/amount/999999.99");

        assertEquals(response.statusCode(), 200, "an empty result is not an error");
        assertTrue(response.jsonPath().getList("$").isEmpty(), "no transaction carries that amount");
    }

    @Test(description = "Reading the transactions of an account that does not exist is not reported as success")
    public void transactionsOfUnknownAccount() {
        Response response = given().spec(api).when().get("/accounts/99999999/transactions");
        boolean emptyOrError = response.statusCode() >= 400 || response.jsonPath().getList("$").isEmpty();
        assertTrue(emptyOrError, "an unknown account must not return someone else's transactions");
    }
}
