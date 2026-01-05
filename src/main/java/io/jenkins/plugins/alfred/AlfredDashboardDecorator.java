package io.jenkins.plugins.alfred;

import hudson.Extension;
import hudson.model.PageDecorator;
import hudson.model.View;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Page decorator that injects Alfred dashboard into view pages
 */
@Extension
public class AlfredDashboardDecorator extends PageDecorator {
    
    public AlfredDashboardDecorator() {
        super();
    }
    
    /**
     * Check if current page is a view page and dashboard is enabled
     */
    public boolean shouldShowDashboard() {
        StaplerRequest request = Stapler.getCurrentRequest();
        if (request == null) {
            return false;
        }

        // Check if we're on a view's main page (not configure, job, or other subpages)
        String requestUri = request.getRequestURI();
        if (requestUri == null) {
            return false;
        }

        // Only show on view main page: /view/{viewName}/ or /view/{viewName}
        // Don't show on: /view/{viewName}/configure, /view/{viewName}/job/*, etc.
        if (!requestUri.matches(".*/view/[^/]+/?$")) {
            return false;
        }

        Object ancestor = request.findAncestorObject(View.class);
        if (ancestor instanceof View) {
            View view = (View) ancestor;
            AlfredDashboardProperty property = view.getProperties().get(AlfredDashboardProperty.class);
            return property != null && property.isEnabled();
        }

        return false;
    }
    
    /**
     * Get the current view
     */
    public View getCurrentView() {
        StaplerRequest request = Stapler.getCurrentRequest();
        if (request != null) {
            Object ancestor = request.findAncestorObject(View.class);
            if (ancestor instanceof View) {
                return (View) ancestor;
            }
        }
        return null;
    }
    
    /**
     * Get dashboard stats for current view
     */
    public AlfredDashboardProperty.DashboardStats getStats() {
        View view = getCurrentView();
        if (view != null) {
            AlfredDashboardProperty property = view.getProperties().get(AlfredDashboardProperty.class);
            if (property != null && property.isEnabled()) {
                return property.getStats(view);
            }
        }
        return new AlfredDashboardProperty.DashboardStats();
    }

    /**
     * Check if Failure Analysis should be shown
     */
    public boolean shouldShowFailureAnalysis() {
        View view = getCurrentView();
        if (view != null) {
            AlfredDashboardProperty property = view.getProperties().get(AlfredDashboardProperty.class);
            if (property != null && property.isEnabled()) {
                return property.isShowFailureAnalysis();
            }
        }
        return false;
    }
}
