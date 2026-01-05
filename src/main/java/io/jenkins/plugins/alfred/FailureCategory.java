package io.jenkins.plugins.alfred;

/**
 * Enumeration of failure categories for pattern matching
 */
public enum FailureCategory {
    SETUP_ISSUES("Setup Issues"),
    API_FAILURES("API Failures"),
    TIMEOUTS("Timeouts"),
    ENVIRONMENT_ISSUES("Environment Issues"),
    JENKINS_CONFIG("Jenkins Configuration"),
    DATABASE_ISSUES("Database Issues"),
    AUTHENTICATION_ISSUES("Authentication/Authorization"),
    NETWORK_ISSUES("Network Issues"),
    TEST_DATA_ISSUES("Test Data Issues"),
    ASSERTION_FAILURES("Assertion Failures"),
    UNKNOWN("Unknown");

    private final String displayName;

    FailureCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
