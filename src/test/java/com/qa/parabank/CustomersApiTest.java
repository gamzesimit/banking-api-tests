package com.qa.parabank;

import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertNotEquals;

public class CustomersApiTest extends BaseApiTest {

    @Test(description = "A customer record carries a name and an address")
    public void customerCarriesTheExpectedShape() {
        given().spec(api)
        .when()
            .get("/customers/" + SEEDED_CUSTOMER_ID)
        .then()
            .statusCode(200)
            .body("id", equalTo(SEEDED_CUSTOMER_ID))
            .body("firstName", not(emptyOrNullString()))
            .body("lastName", not(emptyOrNullString()))
            .body("address.city", not(emptyOrNullString()));
    }

    @Test(description = "A customer record does not expose the password")
    public void customerDoesNotExposeCredentials() {
        String body = given().spec(api)
                .when().get("/customers/" + SEEDED_CUSTOMER_ID)
                .then().statusCode(200)
                .extract().asString();

        org.testng.Assert.assertFalse(body.toLowerCase().contains("\"password\""),
                "a customer record must never carry a password field: " + body);
    }

    @Test(description = "Asking for a customer that does not exist is not reported as success")
    public void unknownCustomerIsNotReportedAsSuccess() {
        Response response = given().spec(api).when().get("/customers/99999999");
        assertNotEquals(response.statusCode(), 200,
                "an unknown customer must not be answered with 200, got body: " + response.asString());
    }
}
