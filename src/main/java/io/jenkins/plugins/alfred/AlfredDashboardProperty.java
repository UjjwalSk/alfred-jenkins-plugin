package io.jenkins.plugins.alfred;

import hudson.Extension;
import hudson.model.*;
import hudson.views.ViewJobFilter;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.*;

/**
 * View property that adds Alfred dashboard to a view
 * Can be enabled/disabled per view via UI or Job DSL
 */
public class AlfredDashboardProperty extends hudson.model.ViewProperty {

    private final boolean enabled;
    private final boolean showFailureAnalysis;

    @DataBoundConstructor
    public AlfredDashboardProperty(boolean enabled, boolean showFailureAnalysis) {
        this.enabled = enabled;
        this.showFailureAnalysis = showFailureAnalysis;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isShowFailureAnalysis() {
        return showFailureAnalysis;
    }

    /**
     * Get dashboard statistics for a given view
     */
    public DashboardStats getStats(View view) {
        DashboardStats stats = new DashboardStats();

        if (view == null) {
            return stats;
        }

        Collection<TopLevelItem> items = view.getItems();

        for (TopLevelItem item : items) {
            if (item instanceof Job) {
                Job<?, ?> job = (Job<?, ?>) item;
                Run<?, ?> lastBuild = job.getLastBuild();

                if (lastBuild != null) {
                    stats.totalJobs++;

                    Result result = lastBuild.getResult();
                    if (result != null) {
                        if (result == Result.SUCCESS) {
                            stats.successfulJobs++;
                        } else if (result == Result.FAILURE) {
                            stats.failedJobs++;
                        } else if (result == Result.UNSTABLE) {
                            stats.unstableJobs++;
                        } else if (result == Result.ABORTED) {
                            stats.abortedJobs++;
                        }
                    }

                    // Collect failure analysis if available
                    AlfredBuildAction action = lastBuild.getAction(AlfredBuildAction.class);
                    if (action != null) {
                        stats.addAnalysis(action.getAnalysisResult());
                    }
                }
            }
        }

        return stats;
    }

    @Extension
    public static class DescriptorImpl extends ViewPropertyDescriptor {
        @Override
        public String getDisplayName() {
            return "Alfred Dashboard";
        }
    }

    /**
     * Dashboard statistics container
     */
    public static class DashboardStats {
        public int totalJobs = 0;
        public int successfulJobs = 0;
        public int failedJobs = 0;
        public int unstableJobs = 0;
        public int abortedJobs = 0;

        private Map<FailureCategory, Integer> categoryCount = new HashMap<>();
        private int totalFailures = 0;

        public void addAnalysis(FailureAnalysisResult result) {
            if (result.getFailedTests() > 0) {
                totalFailures += result.getFailedTests();

                for (FailureCategory category : FailureCategory.values()) {
                    int count = result.getFailureCountForCategory(category);
                    if (count > 0) {
                        categoryCount.merge(category, count, Integer::sum);
                    }
                }
            }
        }

        public Map<FailureCategory, Integer> getCategoryCount() {
            return categoryCount;
        }

        public int getTotalFailures() {
            return totalFailures;
        }

        public FailureCategory getTopCategory() {
            return categoryCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(FailureCategory.UNKNOWN);
        }

        public int getTopCategoryCount() {
            return categoryCount.getOrDefault(getTopCategory(), 0);
        }

        public boolean hasFailures() {
            return totalFailures > 0;
        }
    }
}
