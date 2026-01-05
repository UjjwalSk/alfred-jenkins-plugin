package io.jenkins.plugins.alfred;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates analysis results from multiple builds
 */
public class AggregatedAnalysis implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalFailures = 0;
    private Map<FailureCategory, Integer> categoryCount = new HashMap<>();
    private Map<FailureCategory, List<FailureAnalysisResult.TestFailureInfo>> categoryExamples = new HashMap<>();
    private Map<String, Integer> allFailedApis = new HashMap<>();
    private Map<String, Integer> commonErrors = new HashMap<>();

    public AggregatedAnalysis() {
        for (FailureCategory category : FailureCategory.values()) {
            categoryCount.put(category, 0);
            categoryExamples.put(category, new ArrayList<>());
        }
    }

    public void addResult(FailureAnalysisResult result) {
        totalFailures += result.getFailedTests();

        // Aggregate by category
        for (Map.Entry<FailureCategory, List<FailureAnalysisResult.TestFailureInfo>> entry :
             result.getFailuresByCategory().entrySet()) {

            FailureCategory category = entry.getKey();
            List<FailureAnalysisResult.TestFailureInfo> failures = entry.getValue();

            categoryCount.merge(category, failures.size(), Integer::sum);

            // Keep up to 5 examples per category
            List<FailureAnalysisResult.TestFailureInfo> examples = categoryExamples.get(category);
            for (FailureAnalysisResult.TestFailureInfo failure : failures) {
                if (examples.size() < 5) {
                    examples.add(failure);
                }

                // Track common error messages
                String shortError = failure.getShortError();
                commonErrors.merge(shortError, 1, Integer::sum);
            }
        }

        // Aggregate API failures
        for (Map.Entry<String, Integer> entry : result.getFailedApiEndpoints().entrySet()) {
            allFailedApis.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    public int getTotalFailures() {
        return totalFailures;
    }

    public Map<FailureCategory, Integer> getCategoryCount() {
        return categoryCount;
    }

    public Map<FailureCategory, List<FailureAnalysisResult.TestFailureInfo>> getCategoryExamples() {
        return categoryExamples;
    }

    public List<Map.Entry<String, Integer>> getTopFailedApis(int limit) {
        return allFailedApis.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<Map.Entry<String, Integer>> getTopCommonErrors(int limit) {
        return commonErrors.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    public FailureCategory getTopCategory() {
        return categoryCount.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(FailureCategory.UNKNOWN);
    }

    public int getCategoriesWithFailures() {
        return (int) categoryCount.values().stream().filter(count -> count > 0).count();
    }
}
