Feature: Workload service component scenarios
  As a trainer workload microservice
  I want to aggregate trainer training durations by month
  So that CRM can track trainer workload accurately

  Background:
    Given I have a valid JWT token

  @component
  Scenario: ADD creates workload for a new trainer
    When I send an ADD workload request for trainer "john.doe" named "John" "Doe" active true on "2026-03-10" lasting 60 minutes
    Then the response status should be 200
    When I request the workload summary for "john.doe"
    Then the response status should be 200
    And the summary duration for year 2026 month 3 should be 60

  @component
  Scenario: ADD accumulates duration in the same month
    Given I send an ADD workload request for trainer "jane.smith" named "Jane" "Smith" active true on "2026-03-01" lasting 40 minutes
    When I send an ADD workload request for trainer "jane.smith" named "Jane" "Smith" active true on "2026-03-20" lasting 20 minutes
    Then the response status should be 200
    When I request the workload summary for "jane.smith"
    Then the summary duration for year 2026 month 3 should be 60

  @component
  Scenario: DELETE reduces recorded monthly duration
    Given I send an ADD workload request for trainer "alex.ray" named "Alex" "Ray" active true on "2026-04-05" lasting 90 minutes
    When I send a DELETE workload request for trainer "alex.ray" named "Alex" "Ray" active true on "2026-04-05" lasting 30 minutes
    Then the response status should be 200
    When I request the workload summary for "alex.ray"
    Then the summary duration for year 2026 month 4 should be 60

  @component
  Scenario: DELETE for unknown trainer returns not found
    When I send a DELETE workload request for trainer "ghost.trainer" named "Ghost" "Trainer" active true on "2026-05-01" lasting 30 minutes
    Then the response status should be 404

  @component
  Scenario: DELETE cannot subtract more minutes than recorded
    Given I send an ADD workload request for trainer "sam.lee" named "Sam" "Lee" active true on "2026-06-01" lasting 20 minutes
    When I send a DELETE workload request for trainer "sam.lee" named "Sam" "Lee" active true on "2026-06-01" lasting 50 minutes
    Then the response status should be 400

  @component
  Scenario: Non-positive training duration is rejected
    When I send an ADD workload request for trainer "bad.duration" named "Bad" "Duration" active true on "2026-07-01" lasting 0 minutes
    Then the response status should be 400

  @component
  Scenario: Missing required fields are rejected
    When I send an incomplete ADD workload request missing username
    Then the response status should be 400

  @component
  Scenario: Unauthenticated workload request is rejected
    When I send an ADD workload request for trainer "no.auth" named "No" "Auth" active true on "2026-08-01" lasting 30 minutes without a token
    Then the response status should be 401

  @component
  Scenario: GET summary for unknown trainer returns not found
    When I request the workload summary for "unknown.trainer"
    Then the response status should be 404
