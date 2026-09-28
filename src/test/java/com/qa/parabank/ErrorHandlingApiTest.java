package com.qa.parabank;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * How an endpoint answers a request it cannot serve matters as much as how it
 * answers a good one. A caller that cannot tell a missing record from an empty
 * one has to guess, and guesses end up in production.
 */
public class ErrorHandlingApiTest extends BaseApiTest {

    @Test(description = "A path that does not exist is not answered with a success code")
    public void unknownPathIsNotSuccess() {
        Response response = given().spec(api).when().get("/no-such-endpoint");
        assertTrue(response.statusCode() >= 400,
                "an unknown path must not answer with " + response.statusCode());
    }

    @Test(description = "An identifier that is not a number is refused rather than coerced")
    public void nonNumericIdentifierIsRefused() {
        Response response = given().spec(api).when().get("/accounts/not-a-number");
        assertTrue(response.statusCode() >= 400,
                "a non numeric identifier must be refused, the API answered " + response.statusCode());
    }

    @Test(description = "A transfer with no amount is refused")
    public void transferWithoutAmountIsRefused() {
        Response response = given().spec(api)
                .queryParam("fromAccountId", 12567)
                .queryParam("toAccountId", 12789)
                .when().post("/transfer");

        assertTrue(response.statusCode() >= 400,
                "a transfer with no amount must be refused, the API answered " + response.statusCode());
    }

    @Test(description = "A transfer from an account to itself is refused or is a no operation")
    public void transferToTheSameAccount() {
        java.math.BigDecimal before = new java.math.BigDecimal(given().spec(api)
                .when().get("/accounts/12567")
                .then().statusCode(200)
                .extract().path("balance").toString());

        given().spec(api)
                .queryParam("fromAccountId", 12567)
                .queryParam("toAccountId", 12567)
                .queryParam("amount", "10.00")
                .when().post("/transfer");

        java.math.BigDecimal after = new java.math.BigDecimal(given().spec(api)
                .when().get("/accounts/12567")
                .then().statusCode(200)
                .extract().path("balance").toString());

        assertEquals(after.compareTo(before), 0,
                "a transfer to the same account must leave the balance where it was");
    }
}
