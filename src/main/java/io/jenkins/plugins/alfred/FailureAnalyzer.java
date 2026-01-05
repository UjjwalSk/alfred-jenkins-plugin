package io.jenkins.plugins.alfred;

import hudson.model.Run;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analyzes test failures and categorizes them by pattern matching
 */
public class FailureAnalyzer {

    private static final Map<FailureCategory, List<Pattern>> PATTERNS = new HashMap<>();

    static {
        // Setup Issues
        PATTERNS.put(FailureCategory.SETUP_ISSUES, Arrays.asList(
            Pattern.compile("setup.*fail", Pattern.CASE_INSENSITIVE),
            Pattern.compile("before.*fail", Pattern.CASE_INSENSITIVE),
            Pattern.compile("initialization.*error", Pattern.CASE_INSENSITIVE),
            Pattern.compile("precondition", Pattern.CASE_INSENSITIVE),
            Pattern.compile("@Before", Pattern.CASE_INSENSITIVE),
            Pattern.compile("BeforeClass.*fail", Pattern.CASE_INSENSITIVE),
            Pattern.compile("setUp.*exception", Pattern.CASE_INSENSITIVE)
        ));

        // API Failures
        PATTERNS.put(FailureCategory.API_FAILURES, Arrays.asList(
            Pattern.compile("API|REST|HTTP", Pattern.CASE_INSENSITIVE),
            Pattern.compile("status.*code.*[4-5]\\d{2}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("endpoint", Pattern.CASE_INSENSITIVE),
            Pattern.compile("40[0-9]|50[0-9]"),
            Pattern.compile("unauthorized|forbidden", Pattern.CASE_INSENSITIVE),
            Pattern.compile("not.*found.*404", Pattern.CASE_INSENSITIVE),
            Pattern.compile("internal.*server.*error", Pattern.CASE_INSENSITIVE)
        ));

        // Timeouts
        PATTERNS.put(FailureCategory.TIMEOUTS, Arrays.asList(
            Pattern.compile("timeout", Pattern.CASE_INSENSITIVE),
            Pattern.compile("timed.*out", Pattern.CASE_INSENSITIVE),
            Pattern.compile("TimeoutException"),
            Pattern.compile("SocketTimeout"),
            Pattern.compile("ReadTimeout"),
            Pattern.compile("time.*limit.*exceed", Pattern.CASE_INSENSITIVE)
        ));

        // Environment Issues
        PATTERNS.put(FailureCategory.ENVIRONMENT_ISSUES, Arrays.asList(
            Pattern.compile("environment|env.*variable", Pattern.CASE_INSENSITIVE),
            Pattern.compile("configuration.*missing", Pattern.CASE_INSENSITIVE),
            Pattern.compile("property.*not.*found", Pattern.CASE_INSENSITIVE),
            Pattern.compile("missing.*config", Pattern.CASE_INSENSITIVE),
            Pattern.compile("base.*url.*not.*set", Pattern.CASE_INSENSITIVE)
        ));

        // Jenkins Configuration
        PATTERNS.put(FailureCategory.JENKINS_CONFIG, Arrays.asList(
            Pattern.compile("workspace|maven|gradle", Pattern.CASE_INSENSITIVE),
            Pattern.compile("build.*fail", Pattern.CASE_INSENSITIVE),
            Pattern.compile("compilation", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ClassNotFound", Pattern.CASE_INSENSITIVE),
            Pattern.compile("NoClassDefFound", Pattern.CASE_INSENSITIVE)
        ));

        // Database Issues
        PATTERNS.put(FailureCategory.DATABASE_ISSUES, Arrays.asList(
            Pattern.compile("database|SQL|query", Pattern.CASE_INSENSITIVE),
            Pattern.compile("SQLException"),
            Pattern.compile("connection.*pool", Pattern.CASE_INSENSITIVE),
            Pattern.compile("database.*connection.*fail", Pattern.CASE_INSENSITIVE),
            Pattern.compile("deadlock", Pattern.CASE_INSENSITIVE)
        ));

        // Authentication Issues
        PATTERNS.put(FailureCategory.AUTHENTICATION_ISSUES, Arrays.asList(
            Pattern.compile("auth|403|401", Pattern.CASE_INSENSITIVE),
            Pattern.compile("unauthorized|forbidden", Pattern.CASE_INSENSITIVE),
            Pattern.compile("credentials|token", Pattern.CASE_INSENSITIVE),
            Pattern.compile("authentication.*fail", Pattern.CASE_INSENSITIVE),
            Pattern.compile("access.*denied", Pattern.CASE_INSENSITIVE)
        ));

        // Network Issues
        PATTERNS.put(FailureCategory.NETWORK_ISSUES, Arrays.asList(
            Pattern.compile("network|connection.*refused", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ConnectException"),
            Pattern.compile("UnknownHost"),
            Pattern.compile("no.*route.*to.*host", Pattern.CASE_INSENSITIVE),
            Pattern.compile("SocketException")
        ));

        // Test Data Issues
        PATTERNS.put(FailureCategory.TEST_DATA_ISSUES, Arrays.asList(
            Pattern.compile("test.*data", Pattern.CASE_INSENSITIVE),
            Pattern.compile("missing.*data", Pattern.CASE_INSENSITIVE),
            Pattern.compile("fixture.*data.*missing", Pattern.CASE_INSENSITIVE),
            Pattern.compile("test.*file.*not.*found", Pattern.CASE_INSENSITIVE)
        ));

        // Assertion Failures
        PATTERNS.put(FailureCategory.ASSERTION_FAILURES, Arrays.asList(
            Pattern.compile("assertion|AssertionError", Pattern.CASE_INSENSITIVE),
            Pattern.compile("expected.*but.*was", Pattern.CASE_INSENSITIVE),
            Pattern.compile("verify.*fail", Pattern.CASE_INSENSITIVE),
            Pattern.compile("assert.*fail", Pattern.CASE_INSENSITIVE)
        ));
    }

    /**
     * Analyze a build and categorize its failures
     */
    public FailureAnalysisResult analyze(Run<?, ?> build) {
        FailureAnalysisResult result = new FailureAnalysisResult();

        AbstractTestResultAction testResultAction = build.getAction(AbstractTestResultAction.class);
        if (testResultAction == null) {
            return result;
        }

        Object testResultObj = testResultAction.getResult();
        if (!(testResultObj instanceof TestResult)) {
            return result;
        }

        TestResult testResult = (TestResult) testResultObj;
        result.setTotalTests(testResult.getTotalCount());
        result.setPassedTests(testResult.getPassCount());
        result.setFailedTests(testResult.getFailCount());
        result.setSkippedTests(testResult.getSkipCount());

        // Analyze failed tests
        List<CaseResult> failedTests = testResult.getFailedTests();
        for (CaseResult failedTest : failedTests) {
            String errorMessage = failedTest.getErrorDetails();
            String stackTrace = failedTest.getErrorStackTrace();
            String fullError = (errorMessage != null ? errorMessage : "") + " " +
                             (stackTrace != null ? stackTrace : "");

            FailureCategory category = categorizeFailure(fullError);
            result.addFailure(category, failedTest);

            // Extract API endpoints if it's an API failure
            if (category == FailureCategory.API_FAILURES) {
                extractApiEndpoints(fullError).forEach(result::addFailedApi);
            }
        }

        return result;
    }

    /**
     * Categorize a failure based on error message
     */
    private FailureCategory categorizeFailure(String errorText) {
        for (Map.Entry<FailureCategory, List<Pattern>> entry : PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(errorText).find()) {
                    return entry.getKey();
                }
            }
        }
        return FailureCategory.UNKNOWN;
    }

    /**
     * Extract API endpoints from error messages
     */
    private List<String> extractApiEndpoints(String errorText) {
        List<String> endpoints = new ArrayList<>();
        Pattern[] apiPatterns = {
            Pattern.compile("/(api|v\\d+)/[\\w\\-/]+"),
            Pattern.compile("endpoint[:\\s]+(/[\\w\\-/]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("request.*to[:\\s]+(/[\\w\\-/]+)", Pattern.CASE_INSENSITIVE)
        };

        for (Pattern pattern : apiPatterns) {
            java.util.regex.Matcher matcher = pattern.matcher(errorText);
            while (matcher.find()) {
                String endpoint = matcher.group(matcher.groupCount() > 0 ? 1 : 0);
                if (endpoint != null && endpoint.startsWith("/")) {
                    endpoints.add(endpoint);
                }
            }
        }

        return endpoints;
    }

    /**
     * Aggregate analysis results from multiple builds
     */
    public AggregatedAnalysis aggregateResults(List<FailureAnalysisResult> results) {
        AggregatedAnalysis aggregated = new AggregatedAnalysis();

        for (FailureAnalysisResult result : results) {
            aggregated.addResult(result);
        }

        return aggregated;
    }
}
