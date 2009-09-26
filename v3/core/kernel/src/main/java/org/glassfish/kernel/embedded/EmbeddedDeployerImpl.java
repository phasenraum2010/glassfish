/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.kernel.embedded;

import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.glassfish.api.embedded.EmbeddedDeployer;
import org.glassfish.api.embedded.Server;
import org.glassfish.api.embedded.EmbeddedContainer;
import org.glassfish.api.embedded.EmbeddedFileSystem;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.internal.deployment.SnifferManager;
import org.glassfish.internal.data.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URI;
import java.net.URISyntaxException;
import java.beans.PropertyVetoException;

import com.sun.enterprise.v3.server.ApplicationLifecycle;
import com.sun.enterprise.v3.common.PlainTextActionReporter;
import com.sun.enterprise.v3.admin.CommandRunnerImpl;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.DasConfig;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.logging.LogDomains;

/**
 * @author Jerome Dochez
 */
@Service
public class EmbeddedDeployerImpl implements EmbeddedDeployer {

    @Inject
    Deployment deployment;

    @Inject
    Server server;

    @Inject
    CommandRunner commandRunner;

    @Inject
    Habitat habitat;

    @Inject
    ArchiveFactory factory;

    @Inject
    SnifferManager snifferMgr;

    @Inject
    ServerEnvironment env;

    @Inject
    DasConfig config;

    private static final Logger l = LogDomains.getLogger(EmbeddedDeployerImpl.class, LogDomains.DPL_LOGGER);

    Map<String, EmbeddedDeployedInfo> deployedApps = new HashMap<String, EmbeddedDeployedInfo>();

    final static Logger logger = LogDomains.getLogger(EmbeddedDeployerImpl.class, LogDomains.CORE_LOGGER);

    public File getApplicationsDir() {
        return env.getApplicationRepositoryPath();
    }

    public File getAutoDeployDir() {
        return new File(env.getDomainRoot(), config.getAutodeployDir());
    }

    public void setAutoDeploy(final boolean flag) {

        String value = config.getAutodeployEnabled();
        boolean active = value!=null && Boolean.parseBoolean(
                config.getAutodeployEnabled());
        if (active!=flag) {
            try {
                ConfigSupport.apply(new SingleConfigCode<DasConfig>() {
                    public Object run(DasConfig dasConfig) throws PropertyVetoException, TransactionFailure {
                        dasConfig.setAutodeployEnabled(Boolean.valueOf(flag).toString());
                        return null;
                    }
                }, config);
            } catch(TransactionFailure e) {
                logger.log(Level.SEVERE, "Exception while enabling or disabling the autodeployment of applications", e);
            }
        }
    }

    public String deploy(File archive, DeployCommandParameters params) {
        try {
            ReadableArchive r = factory.openArchive(archive);
            return deploy(r, params);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }

        return null;
    }

    public String deploy(ReadableArchive archive, DeployCommandParameters params) {

        ActionReport report = new PlainTextActionReporter();
        ExtendedDeploymentContext context = null;
        try {
            context = deployment.getBuilder(logger, params, report).source(archive).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(params.property != null){
            context.getAppProps().putAll(params.property);
        }

        if(params.properties != null){
            context.getAppProps().putAll(params.properties);        
        }
        
        final ClassLoader cl = context.getClassLoader();
        Collection<Sniffer> sniffers = snifferMgr.getSniffers(archive, cl);
        List<Sniffer> finalSniffers = new ArrayList<Sniffer>();

        // now we intersect with the conficgured sniffers.
        for (EmbeddedContainer container : server.getContainers()) {
            for (Sniffer sniffer : container.getSniffers()) {
                if (sniffers.contains(sniffer)) {
                    finalSniffers.add(sniffer);            
                }
            }
        }
        ApplicationInfo appInfo = null;
        try {
            appInfo = deployment.deploy(finalSniffers, context);
        } catch(Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        if (appInfo!=null) {
            EmbeddedDeployedInfo info = new EmbeddedDeployedInfo(appInfo, context.getModulePropsMap());
            deployedApps.put(appInfo.getName(), info);
            return appInfo.getName();
        }
        return null;
    }

    public void undeploy(String name, UndeployCommandParameters params) {

        ActionReport report = habitat.getComponent(ActionReport.class, "plain");
        EmbeddedDeployedInfo info = deployedApps.get(name);
        ApplicationInfo appInfo  = info!=null?info.appInfo:null;
        if (appInfo==null) {
            appInfo = deployment.get(name);
        }
        if (appInfo == null) {
            report.setMessage(
                "Cannot find deployed application of name " + name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;            
        }

        ReadableArchive source = appInfo.getSource();
        if (source == null) {
            report.setMessage(
                "Cannot get source archive for undeployment");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (params==null) {
            params = new UndeployCommandParameters(name);
        }
        params.origin = UndeployCommandParameters.Origin.undeploy;
        
        ExtendedDeploymentContext deploymentContext = null;
        try {
            deploymentContext = deployment.getBuilder(logger, params, report).source(source).build();

            if (info!=null) {
                for (ModuleInfo module : appInfo.getModuleInfos()) {
                    info.map.put(module.getName(), module.getModuleProps());
                    deploymentContext.getModuleProps().putAll(module.getModuleProps());
                }
                deploymentContext.setModulePropsMap(info.map);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot create context for undeployment ", e);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }


        deployment.undeploy(name, deploymentContext);


        if (report.getActionExitCode().equals(ActionReport.ExitCode.SUCCESS)) {

            //remove context from generated
            deploymentContext.clean();

        }
        
    }

    public void undeployAll() {
        for (String appName : deployedApps.keySet()) {
            undeploy(appName, null);
        }

    }

    private final static class EmbeddedDeployedInfo {
        final ApplicationInfo appInfo;
        final Map<String, Properties> map;

        public EmbeddedDeployedInfo(ApplicationInfo appInfo, Map<String, Properties> map) {
            this.appInfo = appInfo;
            this.map = map;
        }
    }
}
