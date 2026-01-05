package io.jenkins.plugins.alfred;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.RootAction;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST API endpoint for Alfred analysis
 */
@Extension
public class AlfredApiEndpoint implements RootAction {

    @Override
    public String getIconFileName() {
        return null; // Don't show in UI
    }

    @Override
    public String getDisplayName() {
        return "Alfred API";
    }

    @Override
    public String getUrlName() {
        return "alfred-api";
    }

    /**
     * Get analysis for a specific job
     * URL: /alfred-api/job?name=jobName
     */
    public void doJob(StaplerRequest req, StaplerResponse rsp,
            @QueryParameter String name) throws IOException {
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.READ);

        JSONObject response = new JSONObject();

        Job<?, ?> job = jenkins.getItemByFullName(name, Job.class);
        if (job == null) {
            response.put("error", "Job not found");
            rsp.setStatus(404);
        } else {
            Run<?, ?> lastBuild = job.getLastCompletedBuild();
            if (lastBuild == null) {
                response.put("error", "No completed builds");
                rsp.setStatus(404);
            } else {
                FailureAnalyzer analyzer = new FailureAnalyzer();
                FailureAnalysisResult result = analyzer.analyze(lastBuild);
                response = convertToJson(result);
            }
        }

        rsp.setContentType("application/json");
        rsp.getWriter().write(response.toString());
    }

    /**
     * Get aggregated analysis for multiple jobs in a view
     * URL: /alfred-api/view?name=viewName
     */
    public void doView(StaplerRequest req, StaplerResponse rsp,
            @QueryParameter String name) throws IOException {
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.READ);

        JSONObject response = new JSONObject();

        hudson.model.View view = jenkins.getView(name);
        if (view == null) {
            response.put("error", "View not found");
            rsp.setStatus(404);
        } else {
            List<FailureAnalysisResult> results = new ArrayList<>();
            FailureAnalyzer analyzer = new FailureAnalyzer();

            for (hudson.model.TopLevelItem item : view.getItems()) {
                if (item instanceof Job) {
                    Job<?, ?> job = (Job<?, ?>) item;
                    Run<?, ?> lastBuild = job.getLastCompletedBuild();
                    if (lastBuild != null) {
                        FailureAnalysisResult result = analyzer.analyze(lastBuild);
                        results.add(result);
                    }
                }
            }

            AggregatedAnalysis aggregated = analyzer.aggregateResults(results);
            response = convertAggregatedToJson(aggregated);
        }

        rsp.setContentType("application/json");
        rsp.getWriter().write(response.toString());
    }

    /**
     * Trigger analysis for a specific build
     * URL: /alfred-api/analyze?job=jobName&build=buildNumber
     */
    @RequirePOST
    public void doAnalyze(StaplerRequest req, StaplerResponse rsp,
            @QueryParameter String job,
            @QueryParameter int build) throws IOException {
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.ADMINISTER);

        JSONObject response = new JSONObject();

        Job<?, ?> jobObj = jenkins.getItemByFullName(job, Job.class);
        if (jobObj == null) {
            response.put("error", "Job not found");
            rsp.setStatus(404);
        } else {
            Run<?, ?> run = jobObj.getBuildByNumber(build);
            if (run == null) {
                response.put("error", "Build not found");
                rsp.setStatus(404);
            } else {
                FailureAnalyzer analyzer = new FailureAnalyzer();
                FailureAnalysisResult result = analyzer.analyze(run);

                // Attach to build if not already present
                if (run.getAction(AlfredBuildAction.class) == null) {
                    run.addAction(new AlfredBuildAction(run, result));
                }

                response.put("success", true);
                response.put("analysis", convertToJson(result));
            }
        }

        rsp.setContentType("application/json");
        rsp.getWriter().write(response.toString());
    }

    private JSONObject convertToJson(FailureAnalysisResult result) {
        JSONObject json = new JSONObject();
        json.put("totalTests", result.getTotalTests());
        json.put("passedTests", result.getPassedTests());
        json.put("failedTests", result.getFailedTests());
        json.put("skippedTests", result.getSkippedTests());

        JSONObject categories = new JSONObject();
        for (FailureCategory category : FailureCategory.values()) {
            int count = result.getFailureCountForCategory(category);
            if (count > 0) {
                categories.put(category.name(), count);
            }
        }
        json.put("categories", categories);

        JSONArray failedApis = new JSONArray();
        for (Map.Entry<String, Integer> entry : result.getFailedApiEndpoints().entrySet()) {
            JSONObject api = new JSONObject();
            api.put("endpoint", entry.getKey());
            api.put("count", entry.getValue());
            failedApis.add(api);
        }
        json.put("failedApis", failedApis);

        return json;
    }

    private JSONObject convertAggregatedToJson(AggregatedAnalysis aggregated) {
        JSONObject json = new JSONObject();
        json.put("totalFailures", aggregated.getTotalFailures());
        json.put("categoriesWithFailures", aggregated.getCategoriesWithFailures());
        json.put("topCategory", aggregated.getTopCategory().name());

        JSONObject categories = new JSONObject();
        for (Map.Entry<FailureCategory, Integer> entry : aggregated.getCategoryCount().entrySet()) {
            if (entry.getValue() > 0) {
                categories.put(entry.getKey().name(), entry.getValue());
            }
        }
        json.put("categories", categories);

        JSONArray topApis = new JSONArray();
        for (Map.Entry<String, Integer> entry : aggregated.getTopFailedApis(10)) {
            JSONObject api = new JSONObject();
            api.put("endpoint", entry.getKey());
            api.put("count", entry.getValue());
            topApis.add(api);
        }
        json.put("topFailedApis", topApis);

        return json;
    }
}
