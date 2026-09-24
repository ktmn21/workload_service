package com.crm.workloadservice.cucumber.steps;

import com.crm.workloadservice.cucumber.context.ScenarioContext;
import com.crm.workloadservice.cucumber.support.JwtTestTokenFactory;
import com.crm.workloadservice.dao.TrainerSummaryRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class WorkloadSteps {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private JwtTestTokenFactory jwtTestTokenFactory;

    @Autowired
    private TrainerSummaryRepository trainerSummaryRepository;

    @Before
    public void cleanState() {
        context.clear();
        trainerSummaryRepository.deleteAll();
    }

    @Given("I have a valid JWT token")
    public void createJwt() {
        context.setJwtToken(jwtTestTokenFactory.createToken("cucumber-tester"));
    }

    @Given("I send an ADD workload request for trainer {string} named {string} {string} active {word} on {string} lasting {int} minutes")
    public void sendAdd(String username, String firstName, String lastName, String active, String date, int duration) {
        sendWorkload("ADD", username, firstName, lastName, Boolean.parseBoolean(active), date, duration, true);
    }

    @When("I send a DELETE workload request for trainer {string} named {string} {string} active {word} on {string} lasting {int} minutes")
    public void whenDelete(String username, String firstName, String lastName, String active, String date, int duration) {
        sendWorkload("DELETE", username, firstName, lastName, Boolean.parseBoolean(active), date, duration, true);
    }

    @When("I send an ADD workload request for trainer {string} named {string} {string} active {word} on {string} lasting {int} minutes without a token")
    public void whenAddWithoutToken(String username, String firstName, String lastName, String active, String date, int duration) {
        sendWorkload("ADD", username, firstName, lastName, Boolean.parseBoolean(active), date, duration, false);
    }

    @When("I send an incomplete ADD workload request missing username")
    public void incompleteRequest() {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "Incomplete");
        body.put("lastName", "Request");
        body.put("isActive", true);
        body.put("trainingDate", "2026-09-01");
        body.put("trainingDuration", 30);
        body.put("actionType", "ADD");

        Response response = given()
                .header("Authorization", "Bearer " + context.getJwtToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/workload");
        context.setLastResponse(response);
    }

    @When("I request the workload summary for {string}")
    public void getSummary(String username) {
        Response response = given()
                .header("Authorization", "Bearer " + context.getJwtToken())
                .when()
                .get("/api/workload/{username}", username);
        context.setLastResponse(response);
    }

    @Then("the response status should be {int}")
    public void assertStatus(int status) {
        assertThat(context.getLastResponse().statusCode(), equalTo(status));
    }

    @Then("the summary duration for year {int} month {int} should be {int}")
    public void assertDuration(int year, int month, int expectedDuration) {
        List<Map<String, Object>> years = context.getLastResponse().jsonPath().getList("years");
        Integer actual = years.stream()
                .filter(y -> year == ((Number) y.get("year")).intValue())
                .flatMap(y -> ((List<Map<String, Object>>) y.get("months")).stream())
                .filter(m -> month == ((Number) m.get("month")).intValue())
                .map(m -> ((Number) m.get("trainingSummaryDuration")).intValue())
                .findFirst()
                .orElse(null);
        assertThat(actual, equalTo(expectedDuration));
    }

    private void sendWorkload(String actionType, String username, String firstName, String lastName,
                              boolean active, String date, int duration, boolean withToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("isActive", active);
        body.put("trainingDate", date);
        body.put("trainingDuration", duration);
        body.put("actionType", actionType);

        var request = given().contentType(ContentType.JSON).body(body);
        if (withToken) {
            request = request.header("Authorization", "Bearer " + context.getJwtToken());
        }
        Response response = request.when().post("/api/workload");
        context.setLastResponse(response);
    }
}
