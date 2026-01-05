package io.jenkins.plugins.alfred;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.View;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.verb.GET;

import java.util.logging.Logger;

/**
 * Root action that provides a standalone page for failure analysis
 */
@Extension
public class AlfredFailureAnalysisAction implements RootAction {

    private static final Logger LOGGER = Logger.getLogger(AlfredFailureAnalysisAction.class.getName());

    private String currentViewName;

    @Override
    public String getIconFileName() {
        // Return null to not show in side panel
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Alfred Failure Analysis";
    }

    @Override
    public String getUrlName() {
        return "alfred-analysis";
    }

    /**
     * Get view by name for analysis
     */
    public View getViewByName(String viewName) {
        Jenkins jenkins = Jenkins.get();
        if (viewName == null || viewName.isEmpty()) {
            return jenkins.getPrimaryView();
        }

        View view = jenkins.getView(viewName);
        if (view == null) {
            LOGGER.warning("View not found: " + viewName);
            return jenkins.getPrimaryView();
        }

        return view;
    }

    /**
     * Get the current view name for Jelly template
     */
    public String getViewName() {
        return currentViewName;
    }

    /**
     * Main page handler
     */
    @WebMethod(name = "")
    @GET
    public AlfredFailureAnalysisAction doIndex(@QueryParameter(value = "view", required = false) String viewName) {
        View view = getViewByName(viewName);
        this.currentViewName = viewName != null ? viewName : view.getViewName();

        LOGGER.info("Loading failure analysis page for view: " + this.currentViewName);

        // Return this object, Stapler will automatically render index.jelly
        return this;
    }
}
