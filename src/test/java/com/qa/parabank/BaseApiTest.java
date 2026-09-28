package com.qa.parabank;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;

/**
 * Shared setup for every API test. The base address comes from the BASE_URL
 * environment variable so the same suite runs against a local container and
 * against a deployed environment without a code change.
 */
public abstract class BaseApiTest {

    protected static final String DEFAULT_BASE_URL =
            "http://localhost:8081/parabank/services/bank";

    protected RequestSpecification api;

    @BeforeClass(alwaysRun = true)
    public void setUpApi() {
        String baseUrl = System.getenv().getOrDefault("BASE_URL", DEFAULT_BASE_URL);
        RestAssured.baseURI = baseUrl;
        api = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setAccept(ContentType.JSON)
                .build();
    }

    /** The seeded customer that ships with the application. */
    protected static final int SEEDED_CUSTOMER_ID = 12212;
}
