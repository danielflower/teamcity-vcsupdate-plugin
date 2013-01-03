// Copyright 2009 Jon Vincent
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlecode.vcsupdate;

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.vcs.SVcsRoot;
import jetbrains.buildServer.vcs.VcsManager;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jon Vincent
 */
public final class TeamCityController extends AbstractController {

    private static final String ID_PARAM = "id";
    private static final String NAME_PARAM = "name";
    private static final String SAMPLENAME = "myvcsroot";

    private final WebControllerManager controllerManager;
    private final AuthorizationInterceptor interceptor;
    private final VcsManager vcsManager;
    private final PluginDescriptor descriptor;

    private String viewName = null;
    private String redirectUri = null;

    public TeamCityController(WebControllerManager controllerManager, AuthorizationInterceptor interceptor,
            VcsManager vcsManager, PluginDescriptor descriptor) {
        this.controllerManager = controllerManager;
        this.interceptor = interceptor;
        this.vcsManager = vcsManager;
        this.descriptor = descriptor;
        log("Creating plugin");
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
        try {
            return getModelAndView(request, response);
        } catch (Exception e) {
            log("Error while running plugin: " + e);
            throw new RuntimeException(e);
        }

    }

    private ModelAndView getModelAndView(HttpServletRequest request, HttpServletResponse response) {
        // Get all of the VCS roots specified in the request
        Set<SVcsRoot> roots = new LinkedHashSet<SVcsRoot>();

        // Start by getting any root names from the request
        String[] names = request.getParameterValues(NAME_PARAM);
        if (names != null) {
            for (String name : names) {
                SVcsRoot root = vcsManager.findRootByName(name);
                if (root != null) roots.add(root);
            }
        }

        // Then look for any root IDs
        String[] ids = request.getParameterValues(ID_PARAM);
        if (ids != null) {
            for (String id : ids) {
                try {
                    SVcsRoot root = vcsManager.findRootById(Long.parseLong(id));
                    if (root != null) roots.add(root);
                } catch (NumberFormatException e) {
                    // just move on to the next ID
                }
            }
        }

        // Finally, if the request is a POST but we've found no roots, it may be
        // due to bugs in some POST handling libraries. In this case, we'll
        // update all of the roots.
        if (names == null && ids == null && request.getMethod().equals("POST")) {
            roots.addAll(vcsManager.getAllRegisteredVcsRoots());
        }

        // Did we get a submitted form?
        if (!roots.isEmpty()) {
            // Iterate through the roots
            for (SVcsRoot root : roots) {
                // Find the matching configurations
                List<SBuildType> builds = vcsManager.getAllConfigurationUsages(root);
                if (builds == null) continue;

                // Select the best configuration
                SBuildType selected = null;
                List<SVcsRoot> selectedRoots = null;
                for (SBuildType build : builds) {
                    if (!build.isPaused() && !build.isPersonal()) {
                        List<SVcsRoot> buildRoots = build.getVcsRoots();
                        if (selected == null || buildRoots.size() < selectedRoots.size()) {
                            selected = build;
                            selectedRoots = buildRoots;
                        }
                    }
                }

                // Did we find a match?
                if (selected == null) continue;

                // Kick off the modification check
                boolean defaultInterval = root.isUseDefaultModificationCheckInterval();
                int interval = (defaultInterval ? -1 : root.getModificationCheckInterval());
                root.setModificationCheckInterval(5);
                log("Forcing check for " + selected.getName());
                selected.forceCheckingForChanges();
                if (defaultInterval) {
                    root.restoreDefaultModificationCheckInterval();
                } else {
                    root.setModificationCheckInterval(interval);
                }
            }

            // Redirect to the overview page
            log("Redirecting to: " + redirectUri);
            return new ModelAndView(new RedirectView(redirectUri, true));
        }

        // Build a sample URL
        StringBuilder sampleUrl = new StringBuilder();
        sampleUrl.append(request.getRequestURL()).append('?');
        boolean appendedRoot = false;

        // Append the list of available roots
        List<SVcsRoot> list = vcsManager.getAllRegisteredVcsRoots();
        if (list != null) {
            for (SVcsRoot root : list) {
                if (appendedRoot) sampleUrl.append('&');
                sampleUrl.append(NAME_PARAM).append('=').append(root.getName());
                appendedRoot = true;
            }
        }

        // If we didn't get any roots, use a sample name
        if (!appendedRoot) {
            sampleUrl.append(NAME_PARAM).append('=').append(SAMPLENAME);
        }

        // Return a simple view that explains how to use the tool
        String query = request.getQueryString();
        if (query != null) sampleUrl.append('&').append(query);


        String modelObject = response.encodeURL(sampleUrl.toString());
        log("Creating modelview. View: " + viewName + " and model: " + modelObject);
        return new ModelAndView(viewName, "sampleUrl", modelObject);
    }

    public void setControllerUri(String controllerUri) {
        controllerManager.registerController(controllerUri, this);
        interceptor.addPathNotRequiringAuth(controllerUri);
    }

    public void setViewName(String viewName) {
        this.viewName = descriptor.getPluginResourcesPath(viewName);
    }

    public void setRedirectUri(String redirectUri) {
        log("Setting redirect URI to " + redirectUri);
        Thread.dumpStack();
        this.redirectUri = redirectUri;
    }

    private void log(String message) {
        System.out.println("VCSUPDATEPLUGIN: " + message);
    }

}