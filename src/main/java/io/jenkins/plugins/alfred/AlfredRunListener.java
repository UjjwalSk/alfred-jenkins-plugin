package io.jenkins.plugins.alfred;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens to build completions and automatically runs Alfred analysis
 */
@Extension
public class AlfredRunListener extends RunListener<Run<?, ?>> {
    private static final Logger LOGGER = Logger.getLogger(AlfredRunListener.class.getName());

    @Override
    public void onCompleted(Run<?, ?> run, @NonNull TaskListener listener) {
        try {
            // Only analyze if the build has test results
            if (run.getAction(hudson.tasks.test.AbstractTestResultAction.class) != null) {
                listener.getLogger().println("[Alfred] Analyzing test failures...");

                FailureAnalyzer analyzer = new FailureAnalyzer();
                FailureAnalysisResult result = analyzer.analyze(run);

                // Attach the analysis result to the build
                run.addAction(new AlfredBuildAction(run, result));

                if (result.getFailedTests() > 0) {
                    listener.getLogger().println(
                        String.format("[Alfred] Found %d failed tests across %d categories",
                            result.getFailedTests(),
                            (int) result.getFailuresByCategory().entrySet().stream()
                                .filter(e -> !e.getValue().isEmpty()).count())
                    );

                    // Log top category
                    FailureCategory topCategory = null;
                    int maxCount = 0;
                    for (FailureCategory category : FailureCategory.values()) {
                        int count = result.getFailureCountForCategory(category);
                        if (count > maxCount) {
                            maxCount = count;
                            topCategory = category;
                        }
                    }

                    if (topCategory != null) {
                        listener.getLogger().println(
                            String.format("[Alfred] Top failure category: %s (%d failures)",
                                topCategory.getDisplayName(), maxCount)
                        );
                    }
                } else {
                    listener.getLogger().println("[Alfred] All tests passed!");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to analyze build with Alfred", e);
        }
    }
}
