package io.jenkins.plugins.alfred;

import hudson.tasks.junit.CaseResult;

import java.io.Serializable;
import java.util.*;

/**
 * Holds the analysis results for a single build
 */
public class FailureAnalysisResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalTests = 0;
    private int passedTests = 0;
    private int failedTests = 0;
    private int skippedTests = 0;

    private Map<FailureCategory, List<TestFailureInfo>> failuresByCategory = new HashMap<>();
    private Map<String, Integer> failedApiEndpoints = new HashMap<>();

    public FailureAnalysisResult() {
        // Initialize all categories
        for (FailureCategory category : FailureCategory.values()) {
            failuresByCategory.put(category, new ArrayList<>());
        }
    }

    public void addFailure(FailureCategory category, CaseResult testCase) {
        TestFailureInfo info = new TestFailureInfo(
                testCase.getClassName(),
                testCase.getName(),
                testCase.getErrorDetails(),
                testCase.getErrorStackTrace(),
                testCase.getAge());
        failuresByCategory.get(category).add(info);
    }

    public void addFailedApi(String endpoint) {
        failedApiEndpoints.merge(endpoint, 1, Integer::sum);
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public int getPassedTests() {
        return passedTests;
    }

    public void setPassedTests(int passedTests) {
        this.passedTests = passedTests;
    }

    public int getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    public int getSkippedTests() {
        return skippedTests;
    }

    public void setSkippedTests(int skippedTests) {
        this.skippedTests = skippedTests;
    }

    public Map<FailureCategory, List<TestFailureInfo>> getFailuresByCategory() {
        return failuresByCategory;
    }

    public Map<String, Integer> getFailedApiEndpoints() {
        return failedApiEndpoints;
    }

    public int getFailureCountForCategory(FailureCategory category) {
        return failuresByCategory.get(category).size();
    }

    /**
     * Inner class to hold test failure information
     */
    public static class TestFailureInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String className;
        private final String testName;
        private final String errorDetails;
        private final String stackTrace;
        private final int age;

        public TestFailureInfo(String className, String testName, String errorDetails,
                String stackTrace, int age) {
            this.className = className;
            this.testName = testName;
            this.errorDetails = errorDetails;
            this.stackTrace = stackTrace;
            this.age = age;
        }

        public String getClassName() {
            return className;
        }

        public String getTestName() {
            return testName;
        }

        public String getErrorDetails() {
            return errorDetails;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        public int getAge() {
            return age;
        }

        public String getShortError() {
            if (errorDetails == null)
                return "No error details";
            String[] lines = errorDetails.split("\n");
            return lines.length > 0 ? lines[0].substring(0, Math.min(100, lines[0].length())) : "";
        }
    }
}
