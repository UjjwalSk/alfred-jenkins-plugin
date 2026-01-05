package io.jenkins.plugins.alfred;

import hudson.model.Action;
import hudson.model.Run;

/**
 * Action that attaches Alfred analysis to a build
 */
public class AlfredBuildAction implements Action {
    private final Run<?, ?> build;
    private final FailureAnalysisResult analysisResult;

    public AlfredBuildAction(Run<?, ?> build, FailureAnalysisResult analysisResult) {
        this.build = build;
        this.analysisResult = analysisResult;
    }

    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @Override
    public String getDisplayName() {
        return "Alfred Analysis";
    }

    @Override
    public String getUrlName() {
        return "alfred-analysis";
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public FailureAnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    public AggregatedAnalysis getAggregatedAnalysis() {
        AggregatedAnalysis aggregated = new AggregatedAnalysis();
        aggregated.addResult(analysisResult);
        return aggregated;
    }
}
