package com.qa.parabank;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;

public class AccountsApiTest extends BaseApiTest {

    @Test(description = "An account is returned with the fields a caller depends on")
    public void accountCarriesTheExpectedShape() {
        given().spec(api)
        .when()
            .get("/accounts/12345")
        .then()
            .statusCode(200)
            .body("id", equalTo(12345))
            .body("customerId", notNullValue())
            .body("type", anyOf(equalTo("CHECKING"), equalTo("SAVINGS"), equalTo("LOAN")))
            .body("balance", notNullValue());
    }

    @Test(description = "A balance is returned with two decimal places, not as a rounded figure")
    public void balanceKeepsItsCents() {
        String balance = given().spec(api)
                .when().get("/accounts/12456")
                .then().statusCode(200)
                .extract().path("balance").toString();

        assertTrue(balance.matches("-?\\d+\\.\\d{2}"),
                "a money field must carry exactly two decimal places, got: " + balance);
    }

    @Test(description = "Asking for an account that does not exist does not return a 200 with an empty body")
    public void unknownAccountIsNotReportedAsSuccess() {
        Response response = given().spec(api).when().get("/accounts/99999999");
        assertNotEquals(response.statusCode(), 200,
                "an unknown account must not be answered with 200, got body: " + response.asString());
    }

    @Test(description = "Every account a customer holds belongs to that customer")
    public void everyAccountBelongsToTheCustomerThatWasAsked() {
        List<Integer> owners = given().spec(api)
                .when().get("/customers/" + SEEDED_CUSTOMER_ID + "/accounts")
                .then().statusCode(200)
                .extract().jsonPath().getList("customerId", Integer.class);

        assertFalse(owners.isEmpty(), "the seeded customer must hold at least one account");
        for (Integer owner : owners) {
            assertEquals(owner.intValue(), SEEDED_CUSTOMER_ID,
                    "the list must not contain an account belonging to another customer");
        }
    }

    @Test(description = "The transaction list of an account is returned")
    public void transactionsAreReturnedForAnAccount() {
        given().spec(api)
        .when()
            .get("/accounts/12345/transactions")
        .then()
            .statusCode(200)
            .body("$", instanceOf(List.class));
    }
}
