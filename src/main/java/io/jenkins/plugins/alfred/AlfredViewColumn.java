package io.jenkins.plugins.alfred;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A view column that displays Alfred analysis information
 */
public class AlfredViewColumn extends ListViewColumn {

    @DataBoundConstructor
    public AlfredViewColumn() {
    }

    /**
     * Get the failure analysis for the last completed build of a job
     */
    public FailureAnalysisResult getAnalysis(Job<?, ?> job) {
        Run<?, ?> lastBuild = job.getLastCompletedBuild();
        if (lastBuild == null) {
            return new FailureAnalysisResult();
        }

        AlfredBuildAction action = lastBuild.getAction(AlfredBuildAction.class);
        if (action != null) {
            return action.getAnalysisResult();
        }

        // Generate analysis on-the-fly if not cached
        FailureAnalyzer analyzer = new FailureAnalyzer();
        return analyzer.analyze(lastBuild);
    }

    /**
     * Get a summary string for the job
     */
    public String getSummary(Job<?, ?> job) {
        FailureAnalysisResult analysis = getAnalysis(job);
        if (analysis.getFailedTests() == 0) {
            return "✓ All tests passing";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(analysis.getFailedTests()).append(" failures");

        // Add top category if available
        FailureCategory topCategory = null;
        int maxCount = 0;
        for (FailureCategory category : FailureCategory.values()) {
            int count = analysis.getFailureCountForCategory(category);
            if (count > maxCount) {
                maxCount = count;
                topCategory = category;
            }
        }

        if (topCategory != null && maxCount > 0) {
            sb.append(" (").append(maxCount).append(" ").append(topCategory.getDisplayName()).append(")");
        }

        return sb.toString();
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
            return "Alfred Analysis";
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }
    }
}
