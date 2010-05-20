/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.deployment.versioning;

import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.config.ApplicationName;
import org.glassfish.api.deployment.OpsParams.Origin;
import org.glassfish.api.deployment.StateCommandParameters;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 * This service provides methods to handle application names
 * in the versioning context
 *
 * @author Romain GRECOURT - SERLI (romain.grecourt@serli.com)
 */
@I18n("versioning.service")
@Service
@Scoped(PerLookup.class)
public class VersioningService {

    private static final LocalStringManagerImpl LOCALSTRINGS =
            new LocalStringManagerImpl(VersioningService.class);

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private Applications applications;
    
    static final String REPOSITORY_DASH = "-";
    public static final String EXPRESSION_SEPARATOR = ":";
    public static final String EXPRESSION_WILDCARD = "*";

    /**
     * Extract the untagged name for a given application name that complies
     * with versioning rules (version identifier / expression) or not.
     *
     * If the application name is using versioning rules, the method will split
     * the application names with the semi-colon character and retrieve the
     * untagged name from the first token.
     *
     * Else the given application name is an untagged name.
     * 
     * @param appName the application name
     * @return the untagged version name
     * @throws VersioningSyntaxException if the given application name had some
     * critical patterns.
     */
    public final String getUntaggedName(String appName)
            throws VersioningSyntaxException {

        int semiColonIndex = appName.indexOf(EXPRESSION_SEPARATOR);
        // if versioned
        if (semiColonIndex != -1) {

            // if appName is ending with a semi-colon
            if (semiColonIndex == (appName.length() - 1)) {
                throw new VersioningSyntaxException(
                        LOCALSTRINGS.getLocalString("invalid.appname",
                        "excepted version identifier after semi-colon: {0}",
                        appName));
            }
            return appName.substring(0, semiColonIndex);
        }
        // not versioned
        return appName;
    }

    /**
     * Extract the version identifier / expression for a given application name
     * that complies with versioning rules.
     *
     * The method splits the application name with the semi-colon character
     * and retrieve the 2nd token.
     *
     * @param appName the application name
     * @return the version identifier / expression extracted from application name
     * @throws VersioningSyntaxException if the given application name had some
     * critical patterns.
     */
    public final String getExpression(String appName)
            throws VersioningSyntaxException {

        int semiColonIndex = appName.indexOf(EXPRESSION_SEPARATOR);
        // if versioned
        if (semiColonIndex != -1) {
            if (semiColonIndex != appName.lastIndexOf(EXPRESSION_SEPARATOR)) {
                throw new VersioningSyntaxException(
                        LOCALSTRINGS.getLocalString("invalid.expression",
                        "semi-colon cannot be used twice in version expression/identifier: {0}",
                        appName));
            }
            if (semiColonIndex == (appName.length() - 1)) {
                throw new VersioningSyntaxException(
                        LOCALSTRINGS.getLocalString("invalid.appName",
                        "excepted version expression/identifier after semi-colon: {0}",
                        appName));
            }
            return appName.substring(semiColonIndex + 1, appName.length());
        }
        // not versioned
        return null;
    }

    /**
     * Extract the set of version(s) of the given application from a set of
     * applications. This method is used by unit tests.
     *
     * @param untaggedName the application name as an untagged version : an
     * application name without version identifier
     * @param allApplications the set of applications
     * @return all the version(s) of the given application in the given set of
     * applications
     */
    public List<String> getVersions(String untaggedName,
            List<ApplicationName> allApplications) {
        
        List<String> allVersions = new ArrayList<String>();
        Iterator it = allApplications.iterator();

        while (it.hasNext()) {
            ApplicationName app = ((ApplicationName) it.next());

            // if a tagged version or untagged version of the app
            if (app.getName().startsWith(untaggedName+EXPRESSION_SEPARATOR) ||
                    app.getName().equals(untaggedName)) {
                allVersions.add(app.getName());
            }
        }

        return allVersions;
    }

    /**
     * Extract the set of version(s) of the given application represented as
     * an untagged version name
     *
     * @param untaggedName the application name as an untagged version : an
     * application name without version identifier
     * @return all the version(s) of the given application
     */
    public List<String> getAllversions(String untaggedName){
        List<ApplicationName> allApplications = applications.getModules();
        return getVersions(untaggedName,allApplications);
    }

    /**
     * Search for the enabled version of the given application.
     *
     * @param name the application name
     * @param target an option supply from admin command, it's retained for
     * compatibility with other releases
     * @return the enabled version of the application, if exists
     * @throws VersioningSyntaxException if getUntaggedName throws an exception
     */
    public final String getEnabledVersion(String name, String target)
            throws VersioningSyntaxException {

        String untaggedName = getUntaggedName(name);
        List<String> allVersions = getAllversions(untaggedName);
        
        if (allVersions != null) {
            Iterator it = allVersions.iterator();

            while (it.hasNext()) {
                String app = (String) it.next();

                // if a version of the app is enabled
                if (Boolean.valueOf(
                        ConfigBeansUtilities.getEnabled(target, app))) {

                    return app;
                }
            }
        }
        // no enabled version found
        return null;
    }

    /**
     * Search for the version(s) matched by the expression contained in the given
     * application name. This method is used by unit tests.
     *
     * @param listVersion the set of all versions of the application
     * @param appName the application name containing the expression
     * @return the expression matched list
     * @throws VersioningException if the expression is an identifier matching
     * a version not registred, or if getExpression throws an exception
     */
    public final List<String> matchExpression(List<String> listVersion, String appName)
            throws VersioningException {

        if (listVersion.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        String expressionVersion = getExpression(appName);
        List<String> matchedVersions = new ArrayList<String>(listVersion);

        // if using an untagged version
        if (expressionVersion == null) {
            // return the matched version if exist
            if (listVersion.contains(appName)) {
                return listVersion.subList(listVersion.indexOf(appName),
                        listVersion.indexOf(appName) + 1);
            } else {
                throw new VersioningException(
                    LOCALSTRINGS.getLocalString("version.notreg",
                    "version {0} not registred",
                    appName));
            }
        }

        // if using an identifier
        if (expressionVersion.indexOf(EXPRESSION_WILDCARD) == -1) {
            // return the matched version if exist
            if (listVersion.contains(appName)) {
                return listVersion.subList(listVersion.indexOf(appName),
                        listVersion.indexOf(appName) + 1);
            } else {
                throw new VersioningException(
                    LOCALSTRINGS.getLocalString("version.notreg",
                    "Version {0} not registred",
                    appName));
            }
        }

        StringTokenizer st = new StringTokenizer(expressionVersion,
                EXPRESSION_WILDCARD);
        String lastToken = null;

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            Iterator it = new ArrayList<String>(matchedVersions).iterator();

            while (it.hasNext()) {
                String app = (String) it.next();
                String expression = getExpression(app);

                // get the position of the last token in the expression
                int tokenCursor = 0;
                if (lastToken != null) {
                    tokenCursor = expression.indexOf(lastToken) + 1;
                }

                // expression is null for untagged version
                // if token not found the version is not matching the expression
                if (expression == null
                        || expression.indexOf(token, tokenCursor) == -1) {
                    // remove unmatched version
                    matchedVersions.remove(app);
                }
            }
            lastToken = token;
        }

        // returns matched version(s)
        return matchedVersions;
    }

    /**
     * Process the expression matching operation of the given application name.
     *
     * @param name the application name containing the version expression
     * @return a List of all expression matched versions
     * @throws VersioningException if the application has no version registred,
     * or if getUntaggedName throws an exception
     */
    public final List<String> getMatchedVersions(String name)
            throws VersioningException {

        String untagged = getUntaggedName(name);
        List<String> allVersions = getAllversions(untagged);

        if(allVersions.size() == 0){
            throw new VersioningException(
                LOCALSTRINGS.getLocalString("application.noversion",
                "Application {0} has no version registred",
                untagged));
        }
        
        return matchExpression(allVersions, name);
    }

    /**
     * Replaces the semi-colon with a dash in the given application name.
     *
     * @param appName
     * @return return a valid repository name
     * @throws VersioningSyntaxException if getEpression and getUntaggedName
     * throws exception
     */
    public final String getRepositoryName(String appName)
            throws VersioningSyntaxException {

        String expression = getExpression(appName);
        String untaggedName = getUntaggedName(appName);

        if (expression != null) {

            StringBuilder repositoryNameBuilder = new StringBuilder(untaggedName);
            repositoryNameBuilder.append(REPOSITORY_DASH);
            repositoryNameBuilder.append(expression);
            return repositoryNameBuilder.toString();

        } else {
            return untaggedName;
        }
    }

    /**
     *  Disable the enabled version of the application if it exists. This method
     *  is used in versioning context.
     *
     *  @param appName application's name
     *  @param target an option supply from admin command, it's retained for
     * compatibility with other releases
     *  @param report ActionReport, report object to send back to client.
     */
    public void handleDisable(final String appName, final String target,
            final ActionReport report) throws VersioningSyntaxException {

        StateCommandParameters commandParams = new StateCommandParameters();
        // retrieve the currently enabled version of the application
        String enabledVersion = getEnabledVersion(appName, target);

        // invoke disable if the currently enabled version is not itself
        if(enabledVersion != null &&
                  !enabledVersion.equals(appName)){
            commandParams.component = enabledVersion;
            commandParams.origin = Origin.load;
            commandParams.target = target;

            ActionReport subReport = report.addSubActionsReport();

            CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation("disable", subReport);
            inv.parameters(commandParams).execute();
        }
    }
}
