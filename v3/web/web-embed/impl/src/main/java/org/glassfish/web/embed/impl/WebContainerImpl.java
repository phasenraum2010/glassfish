/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package org.glassfish.web.embed.impl;

import java.beans.PropertyVetoException;
import java.io.File;

import java.util.*;
import java.util.logging.*;

import com.sun.enterprise.web.EmbeddedWebContainer;
import com.sun.enterprise.web.WebModule;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.grizzly.config.dom.FileCache;
import com.sun.grizzly.config.dom.Http;
import com.sun.grizzly.config.dom.NetworkConfig;
import com.sun.grizzly.config.dom.NetworkListener;
import com.sun.grizzly.config.dom.NetworkListeners;
import com.sun.grizzly.config.dom.Protocol;
import com.sun.grizzly.config.dom.Protocols;
import com.sun.grizzly.config.dom.ThreadPool;
import com.sun.grizzly.config.dom.Transport;
import com.sun.grizzly.config.dom.Transports;

import org.glassfish.api.container.Sniffer;
import org.glassfish.api.embedded.Port;
import org.glassfish.api.embedded.Ports;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.web.ConfigException;
import org.glassfish.embeddable.web.Context;
import org.glassfish.embeddable.web.WebContainer;
import org.glassfish.embeddable.web.HttpListener;
import org.glassfish.embeddable.web.VirtualServer;
import org.glassfish.embeddable.web.WebListener;
import org.glassfish.embeddable.web.config.WebContainerConfig;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.annotations.ContractProvided;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Realm;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.ContextConfig;


/**
 * Class representing an embedded web container, which supports the
 * programmatic creation of different types of web protocol listeners
 * and virtual servers, and the registration of static and dynamic
 * web resources into the URI namespace.
 *  
 * @author Amy Roh
 */
@Service
@ContractProvided(WebContainer.class)
public class WebContainerImpl implements WebContainer {

    Inhabitant<? extends org.glassfish.api.container.Container> container;

    Inhabitant<?> embeddedInhabitant;

    @Inject
    Habitat habitat;

    @Inject
    HttpService httpService;

    private static Logger log =
            Logger.getLogger(WebContainerImpl.class.getName());

    @Inject
    NetworkConfig networkConfig;

    @Inject
    ServerContext serverContext;   

    // ----------------------------------------------------- Instance Variables

    private WebContainerConfig config;
    
    private EmbeddedWebContainer embedded;
    
    private Engine engine = null;

    private boolean initialized = false;

    private String listenerName = "embedded-listener";

    private List<WebListener> listeners = new ArrayList<WebListener>();

    private File path = null;

    private String securityEnabled = "false";

    private com.sun.enterprise.web.WebContainer webContainer; 

    // --------------------------------------------------------- Public Methods

    private void init() {
        if (initialized) {
            return;
        }
        if (config == null) {
            // use default settings
            config = new WebContainerConfig();
        }
        container = habitat.getInhabitant(org.glassfish.api.container.Container.class,
                "com.sun.enterprise.web.WebContainer");
        if (container==null) {
            log.severe("Cannot find webcontainer implementation");
        }
        embeddedInhabitant = habitat.getInhabitantByType("com.sun.enterprise.web.EmbeddedWebContainer");
        if (embeddedInhabitant==null) {
            log.severe("Cannot find embedded implementation");
        }
        try {
            webContainer = (com.sun.enterprise.web.WebContainer) container.get();
            embedded = (EmbeddedWebContainer) embeddedInhabitant.get();
            engine = webContainer.getEngine();
            if (engine == null) {
                log.severe("Cannot find engine implementation");
            }
            initialized = true;
        } catch (Exception e) {
            log.severe("Init exception "+e.getMessage());
        }
    }

    public void setConfiguration(WebContainerConfig config) {
        if (!initialized) {
            init();
        }
        this.config = config;
        final WebContainerConfig webConfig = config;
        com.sun.enterprise.config.serverbeans.VirtualServer vsBean =
                httpService.getVirtualServerByName(config.getVirtualServerId());
        try {
            if (vsBean!=null) {
                ConfigSupport.apply(new SingleConfigCode<com.sun.enterprise.config.serverbeans.VirtualServer>() {
                    public Object run(com.sun.enterprise.config.serverbeans.VirtualServer avs)
                            throws PropertyVetoException, TransactionFailure {
                        avs.setId(webConfig.getVirtualServerId());
                        if (webConfig.getDocRootDir() != null) {
                            avs.setDocroot(webConfig.getDocRootDir().getPath());
                        }
                        avs.setHosts(webConfig.getHostNames());
                        avs.setNetworkListeners(webConfig.getListenerName());
                        return avs;
                    }
                }, vsBean);
            }
            embedded.setDirectoryListing(config.getListings());
            if (findWebListener(config.getPort())==null) {
                Ports ports = habitat.getComponent(Ports.class);
                Port port = ports.createPort(webConfig.getPort());
                bind(port, null);
            }
        }  catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the list of sniffers associated with this embedded container
     * @return list of sniffers
     */
    public List<Sniffer> getSniffers() {
        List<Sniffer> sniffers = new ArrayList<Sniffer>();
        sniffers.add(habitat.getComponent(Sniffer.class, "web"));
        sniffers.add(habitat.getComponent(Sniffer.class, "weld"));        
        Sniffer security = habitat.getComponent(Sniffer.class, "Security");
        if (security!=null) {
            sniffers.add(security);
        }
        return sniffers;
    }

    private void bind(Port port, WebListener webListener) {
        final int portNumber = port.getPortNumber();
        String protocol = Port.HTTP_PROTOCOL;

        if (webListener==null) {
            listenerName = getListenerName();
            webListener = new HttpListener();
            webListener.setId(listenerName);
            webListener.setPort(portNumber);
        } else {
            listenerName = webListener.getId();
            protocol = webListener.getProtocol();
        }
        listeners.add(webListener);

        log.info("WebContainer binding port "+port.getPortNumber()+" protocol "+protocol);

        if (protocol.equals(Port.HTTP_PROTOCOL)) {
            securityEnabled = "false";
        } else if (protocol.equals(Port.HTTPS_PROTOCOL)) {
            securityEnabled = "true";
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<Protocols>() {
                public Object run(Protocols param) throws TransactionFailure {
                    final Protocol protocol = param.createChild(Protocol.class);
                    protocol.setName(listenerName);
                    protocol.setSecurityEnabled(securityEnabled);
                    param.getProtocol().add(protocol);
                    final Http http = protocol.createChild(Http.class);
                    http.setDefaultVirtualServer(config.getVirtualServerId());
                    http.setFileCache(http.createChild(FileCache.class));
                    protocol.setHttp(http);
                    return protocol;
                }
            }, networkConfig.getProtocols());
            ConfigSupport.apply(new ConfigCode() {
                public Object run(ConfigBeanProxy... params) throws TransactionFailure {
                    NetworkListeners nls = (NetworkListeners) params[0];
                    Transports transports = (Transports) params[1];
                    final NetworkListener listener = nls.createChild(NetworkListener.class);
                    listener.setName(listenerName);
                    listener.setPort(Integer.toString(portNumber));
                    listener.setProtocol(listenerName);
                    listener.setThreadPool("http-thread-pool");
                    if (listener.findThreadPool() == null) {
                        final ThreadPool pool = nls.createChild(ThreadPool.class);
                        pool.setName(listenerName);
                        listener.setThreadPool(listenerName);
                    }
                    listener.setTransport("tcp");
                    if (listener.findTransport() == null) {
                        final Transport transport = transports.createChild(Transport.class);
                        transport.setName(listenerName);
                        listener.setTransport(listenerName);
                    }
                    nls.getNetworkListener().add(listener);
                    return listener;
                }
            }, networkConfig.getNetworkListeners(), networkConfig.getTransports());

            com.sun.enterprise.config.serverbeans.VirtualServer vs =
                    httpService.getVirtualServerByName(config.getVirtualServerId());
            ConfigSupport.apply(new SingleConfigCode<com.sun.enterprise.config.serverbeans.VirtualServer>() {
                public Object run(com.sun.enterprise.config.serverbeans.VirtualServer avs) throws PropertyVetoException {
                    avs.addNetworkListener(listenerName);
                    return avs;
                }
            }, vs);
        } catch (Exception e) {
            removeListener(listenerName);
            e.printStackTrace();
        }
    }

    private String getListenerName() {
        int i = 1;
        // use listenerName set via addWebListener
        if (!existsListener(listenerName)) {
            return listenerName;
        }
        // use default listener name
        String name = "embedded-listener";
        while (existsListener(name)) {
            name = "embedded-listener-" + i++;
        }
        return name;
    }

    private boolean existsListener(String lName) {
        for (NetworkListener nl : networkConfig.getNetworkListeners().getNetworkListener()) {
            if (nl.getName().equals(lName)) {
                return true;
            }
        }
        return false;
    }

    private void removeListener(String name) {
        final String listenerName = name;

        try {
            NetworkListeners networkListeners = networkConfig.getNetworkListeners();
            final NetworkListener listenerToBeRemoved = networkConfig.getNetworkListener(listenerName);
            if (listenerToBeRemoved == null) {
                log.severe("Network Listener "+listenerName+" doesn't exist");
            } else {
                final com.sun.enterprise.config.serverbeans.VirtualServer virtualServer =
                        httpService.getVirtualServerByName(
                        listenerToBeRemoved.findHttpProtocol().getHttp().getDefaultVirtualServer());
                ConfigSupport.apply(new ConfigCode() {
                    public Object run(ConfigBeanProxy... params) throws PropertyVetoException {
                        final NetworkListeners listeners = (NetworkListeners) params[0];
                        final com.sun.enterprise.config.serverbeans.VirtualServer server =
                                (com.sun.enterprise.config.serverbeans.VirtualServer) params[1];
                        listeners.getNetworkListener().remove(listenerToBeRemoved);
                        server.removeNetworkListener(listenerToBeRemoved.getName());
                        return listenerToBeRemoved;
                    }
                }, networkListeners, virtualServer);

            }
        } catch (TransactionFailure e) {
            log.severe("Remove listener "+name+" failed "+e.getMessage());
        }
    }

    /**
     * Creates a <tt>Context</tt> and configures it with the given
     * docroot and classloader.
     *
     * <p>The classloader of the class on which this method is called
     * will be used.
     *
     * <p>In order to access the new <tt>Context</tt> or any of its
     * resources, the <tt>Context</tt> must be registered with a
     * <tt>VirtualServer</tt> that has been started.
     *
     * @param docRoot the docroot of the <tt>Context</tt>
     *
     * @return the new <tt>Context</tt>
     *
     * @see VirtualServer#addContext
     */
    public Context createContext(File docRoot) {
        return createContext(docRoot, null);
    }
    
    /**
     * Creates a <tt>Context</tt>, configures it with the given
     * docroot and classloader, and registers it with the default
     * <tt>VirtualServer</tt>.
     *
     * <p>The given classloader will be set as the thread's context
     * classloader whenever the new <tt>Context</tt> or any of its
     * resources are asked to process a request.
     * If a <tt>null</tt> classloader is passed, the classloader of the
     * class on which this method is called will be used.
     *
     * @param docRoot the docroot of the <tt>Context</tt>
     * @param contextRoot
     * @param classLoader the classloader of the <tt>Context</tt>
     *
     * @return the new <tt>Context</tt>
     */
    public Context createContext(File docRoot, String contextRoot, 
            ClassLoader classLoader) {
        Context context = createContext(docRoot, null);
        try {
            for (VirtualServer vs : getVirtualServers()) {
                vs.addContext(context, contextRoot);
            }
        } catch (Exception ex) {
            log.severe("Couldn't add context "+context+" using "+contextRoot);
            ex.printStackTrace();
        }
        return context;
    }

    /**
     * Creates a <tt>Context</tt> and configures it with the given
     * docroot and classloader.
     *
     * <p>The given classloader will be set as the thread's context
     * classloader whenever the new <tt>Context</tt> or any of its
     * resources are asked to process a request.
     * If a <tt>null</tt> classloader is passed, the classloader of the
     * class on which this method is called will be used.
     *
     * <p>In order to access the new <tt>Context</tt> or any of its 
     * resources, the <tt>Context</tt> must be registered with a
     * <tt>VirtualServer</tt> that has been started.
     *
     * @param docRoot the docroot of the <tt>Context</tt>
     * @param classLoader the classloader of the <tt>Context</tt>
     *
     * @return the new <tt>Context</tt>
     *
     * @see VirtualServer#addContext
     */
    public Context createContext(File docRoot, ClassLoader classLoader) {
        if (!initialized) {
            init();
        }
        if (log.isLoggable(Level.INFO)) {
            log.info("Creating context '"+docRoot.getName()+"' with docBase '" +
                    docRoot.getPath() + "'");
        }

        String appName;
        WebModule context = null;
        try {
            Deployer deployer = habitat.getComponent(Deployer.class);
            appName = deployer.deploy(docRoot, "--name", docRoot.getName());
            if (!appName.startsWith("/")) {
                appName = "/"+appName;
            }
            VirtualServer vs = findVirtualServer("server");
            context = (WebModule) ((StandardHost)vs).findChild(appName);
            if (context != null) {
                ((StandardHost)vs).removeChild(context);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return context;
    }

    /**
     * Registers the given <tt>Context</tt> with all <tt>VirtualServer</tt>
     * at the given context root.
     *
     * <p>If <tt>VirtualServer</tt> has already been started, the
     * given <tt>context</tt> will be started as well.
     *
     * @param context the <tt>Context</tt> to register
     * @param contextRoot the context root at which to register
     *
     * @throws org.glassfish.embeddable.web.ConfigException if a <tt>Context</tt> already exists
     * at the given context root on <tt>VirtualServer</tt>
     * @throws GlassFishException if the given <tt>context</tt> fails
     * to be started
     */
    public void addContext(Context context, String contextRoot)
            throws ConfigException, GlassFishException {
        if (!contextRoot.startsWith("/")) {
            contextRoot = "/"+ contextRoot;
        }
        for (VirtualServer vs : getVirtualServers()) {
            if (vs.findContext(contextRoot)!=null) {
                throw new ConfigException("Context with contextRoot "+
                        contextRoot+" is already registered");
            }
            vs.addContext(context, contextRoot);
        }
    }

    /**
     * Creates a <tt>WebListener</tt> from the given class type and
     * assigns the given id to it.
     *
     * @param id the id of the new <tt>WebListener</tt>
     * @param c the class from which to instantiate the
     * <tt>WebListener</tt>
     * 
     * @return the new <tt>WebListener</tt> instance
     *
     * @throws  IllegalAccessException if the given <tt>Class</tt> or
     * its nullary constructor is not accessible.
     * @throws  InstantiationException if the given <tt>Class</tt>
     * represents an abstract class, an interface, an array class,
     * a primitive type, or void; or if the class has no nullary
     * constructor; or if the instantiation fails for some other reason.
     * @throws ExceptionInInitializerError if the initialization
     * fails
     * @throws SecurityException if a security manager, <i>s</i>, is
     * present and any of the following conditions is met:
     *
     * <ul>
     * <li> invocation of <tt>{@link SecurityManager#checkMemberAccess
     * s.checkMemberAccess(this, Member.PUBLIC)}</tt> denies
     * creation of new instances of the given <tt>Class</tt>
     * <li> the caller's class loader is not the same as or an
     * ancestor of the class loader for the current class and
     * invocation of <tt>{@link SecurityManager#checkPackageAccess
     * s.checkPackageAccess()}</tt> denies access to the package
     * of this class
     * </ul>
     */
    public <T extends WebListener> T createWebListener(String id, Class<T> c) 
            throws InstantiationException, IllegalAccessException {
        
        T webListener = null;
        if (log.isLoggable(Level.INFO)) {
            log.info("Creating connector "+id);
        }

        try {
            webListener = c.newInstance();
            webListener.setId(id);
        } catch (Exception e) {
            log.severe("Couldn't create connector "+e.getMessage());
        }

        return webListener;

    }

    /**
     * Adds the given <tt>WebListener</tt> to this
     * <tt>WebContainer</tt>.
     *
     * <p>If this <tt>WebContainer</tt> has already been started,
     * the given <tt>webListener</tt> will be started as well.
     *
     * @param webListener the <tt>WebListener</tt> to add
     *
     * @throws ConfigException if a <tt>WebListener</tt> with the
     * same id has already been registered with this
     * <tt>WebContainer</tt>
     * @throws GlassFishException if the given <tt>webListener</tt> fails
     * to be started
     */
    public void addWebListener(WebListener webListener) 
            throws ConfigException, GlassFishException {

        if (!initialized) {
            init();
        }

        if (findWebListener(webListener.getId())==null) {
            listenerName = webListener.getId();
        } else {
            throw new ConfigException("Connector with name '"+
                    webListener.getId()+"' already exsits");           
        }
        
        if (log.isLoggable(Level.INFO)) {
            log.info("Added connector "+webListener.getId());
        }

        listenerName = webListener.getId();
        try {
            Ports ports = habitat.getComponent(Ports.class);
            Port port = ports.createPort(webListener.getPort());
            bind(port, webListener);
        } catch (java.io.IOException ex) {
            throw new ConfigException(ex);
        }
        
    }

    /**
     * Finds the <tt>WebListener</tt> with the given id.
     *
     * @param id the id of the <tt>WebListener</tt> to find
     *
     * @return the <tt>WebListener</tt> with the given id, or
     * <tt>null</tt> if no <tt>WebListener</tt> with that id has been
     * registered with this <tt>WebContainer</tt>
     */
    public WebListener findWebListener(String id) {
        if (!initialized) {
            init();
        }
        for (WebListener listener : listeners) {
            if (listener.getId().equals(id)) {
                return listener;
            }
        }
        return null;
    }

    private WebListener findWebListener(int port) {
        for (WebListener listener : listeners) {
            if (listener.getPort() == port) {
                return listener;
            }
        }
        return null;
    }

    /**
     * Gets the collection of <tt>WebListener</tt> instances registered
     * with this <tt>WebContainer</tt>.
     * 
     * @return the (possibly empty) collection of <tt>WebListener</tt>
     * instances registered with this <tt>WebContainer</tt>
     */
    public Collection<WebListener> getWebListeners() {     
        return listeners;
    }

    /**
     * Stops the given <tt>webListener</tt> and removes it from this
     * <tt>WebContainer</tt>.
     *
     * @param webListener the <tt>WebListener</tt> to be stopped
     * and removed
     *
     * @throws GlassFishException if an error occurs during the stopping
     * or removal of the given <tt>webListener</tt>
     */
    public void removeWebListener(WebListener webListener)
        throws GlassFishException {

        if (listeners.contains(webListener)) {
            listeners.remove(webListener);
        } else {
            throw new GlassFishException(new ConfigException("Connector with name '"+
                    webListener.getId()+"' does not exsits"));
        }

        removeListener(webListener.getId());
    }

    /**
     * Creates a <tt>VirtualServer</tt> with the given id and docroot, and
     * maps it to the given <tt>WebListener</tt> instances.
     * 
     * @param id the id of the <tt>VirtualServer</tt>
     * @param docRoot the docroot of the <tt>VirtualServer</tt>
     * @param webListeners the list of <tt>WebListener</tt> instances from 
     * which the <tt>VirtualServer</tt> will receive requests
     * 
     * @return the new <tt>VirtualServer</tt> instance
     */
    public VirtualServer createVirtualServer(String id,
        File docRoot, WebListener...  webListeners) {

        com.sun.enterprise.web.VirtualServer virtualServer =
                new com.sun.enterprise.web.VirtualServer();
        virtualServer.setID(id);
        virtualServer.setName(id);
        if (docRoot!=null) {
            virtualServer.setAppBase(docRoot.getPath());
        }

        List<String> names = new ArrayList<String>();
        for (WebListener listener : webListeners) {
            names.add(listener.getId());
        }
        virtualServer.setNetworkListenerNames(names.toArray(new String[names.size()]));
        virtualServer.setWebListeners(webListeners);

        if (log.isLoggable(Level.INFO)) {
            log.info("Created virtual server "+id+" docroot "+docRoot.getPath()+
                    " networklisteners "+virtualServer.getNetworkListeners());
        }

        return virtualServer;
        
    }
    
    /**
     * Creates a <tt>VirtualServer</tt> with the given id and docroot, and
     * maps it to all <tt>WebListener</tt> instances.
     * 
     * @param id the id of the <tt>VirtualServer</tt>
     * @param docRoot the docroot of the <tt>VirtualServer</tt>
     * 
     * @return the new <tt>VirtualServer</tt> instance
     */    
    public VirtualServer createVirtualServer(String id, File docRoot) {

        com.sun.enterprise.web.VirtualServer virtualServer =
                new com.sun.enterprise.web.VirtualServer();
        virtualServer.setID(id);
        virtualServer.setName(id);
        if (docRoot!=null) {
            virtualServer.setAppBase(docRoot.getPath());
        }

        List<String> networkListenerNames = new ArrayList<String>();
        for (NetworkListener networkListener :
                networkConfig.getNetworkListeners().getNetworkListener()) {
            networkListenerNames.add(networkListener.getName());

        }
        virtualServer.setNetworkListenerNames(
                networkListenerNames.toArray(new String[networkListenerNames.size()]));

        if (log.isLoggable(Level.INFO)) {
            log.info("Created virtual server "+id+" docroot "+docRoot.getPath()+
                    " networklisteners "+virtualServer.getNetworkListeners());
        }

        return virtualServer;
        
    }

    /**
     * Adds the given <tt>VirtualServer</tt> to this
     * <tt>WebContainer</tt>.
     *
     * <p>If this <tt>WebContainer</tt> has already been started,
     * the given <tt>virtualServer</tt> will be started as well.
     *
     * @param virtualServer the <tt>VirtualServer</tt> to add
     *
     * @throws ConfigException if a <tt>VirtualServer</tt> with the
     * same id has already been registered with this
     * <tt>WebContainer</tt>
     * @throws GlassFishException if the given <tt>virtualServer</tt> fails
     * to be started
     */
    public void addVirtualServer(VirtualServer virtualServer)
        throws ConfigException, GlassFishException {
        if (!initialized) {
            init();
        }
        final String virtualServerId = virtualServer.getID();
        final String networkListeners = ((StandardHost)virtualServer).getNetworkListeners();
        final String docRoot = virtualServer.getDocRoot().getPath();
        String hostNames = "${com.sun.aas.hostName}";
        if (virtualServer.getConfig()!=null) {
            hostNames = virtualServer.getConfig().getHostNames();
        }
        final String hosts = hostNames;

        for (com.sun.enterprise.config.serverbeans.VirtualServer vs: httpService.getVirtualServer()) {
            if (virtualServerId.equals(vs.getId())) {
                throw new ConfigException("VirtualServer with id "+
                        virtualServerId+" is already registered");
            }
        }
        try {
            for (WebListener listener : virtualServer.getWebListeners()) {
                addWebListener(listener);
            }
            engine.addChild((StandardHost)virtualServer);
            /*ConfigSupport.apply(new SingleConfigCode<HttpService>() {
                public Object run(HttpService param) throws PropertyVetoException, TransactionFailure {
                    com.sun.enterprise.config.serverbeans.VirtualServer newVirtualServer =
                            param.createChild(com.sun.enterprise.config.serverbeans.VirtualServer.class);
                    newVirtualServer.setId(virtualServerId);
                    newVirtualServer.setHosts(hosts);
                    newVirtualServer.setNetworkListeners(networkListeners);
                    Property property = newVirtualServer.createChild(Property.class);
                    property.setName("docroot");
                    property.setValue(docRoot);
                    newVirtualServer.getProperty().add(property);
                    param.getVirtualServer().add(newVirtualServer);
                    return newVirtualServer;
                }
            }, httpService);*/
        } catch (Exception ex) {
            throw new GlassFishException(ex);
        }
        com.sun.enterprise.web.VirtualServer vs =
                (com.sun.enterprise.web.VirtualServer) engine.findChild(virtualServerId);

        /*WebModuleConfig wmInfo = vs.createSystemDefaultWebModuleIfNecessary(
                habitat.getComponent(WebArchivist.class));
        String defaultPath = null;
        if (wmInfo != null) {
            defaultPath = wmInfo.getContextPath();
            webContainer.loadStandaloneWebModule(vs, wmInfo);
        }
                if (_logger.isLoggable(Level.INFO)) {
                    _logger.log(Level.INFO,
                            "webContainer.virtualServer.loadedDefaultWebModule",
                            new Object[]{vs.getName(), defaultPath});
                }  */
        if (vs != null) {
            if (log.isLoggable(Level.INFO)) {
                log.info("Added virtual server "+vs.getName()+" with networklisteners "+networkListeners);
                //log.info("Virtual Server "+vs.getName()+" loaded default web module "+defaultPath);
            }
        } else {
            log.severe("Cannot add virtual server "+virtualServerId);
            throw new GlassFishException(new Exception("Cannot add virtual server "+virtualServerId));
        }
        
    }

    /**
     * Finds the <tt>VirtualServer</tt> with the given id.
     *
     * @param id the id of the <tt>VirtualServer</tt> to find
     *
     * @return the <tt>VirtualServer</tt> with the given id, or
     * <tt>null</tt> if no <tt>VirtualServer</tt> with that id has been
     * registered with this <tt>WebContainer</tt>
     */
    public VirtualServer findVirtualServer(String id) {
        if (!initialized) {
            init();
        }
        return (VirtualServer)engine.findChild(id);
    }

    /**
     * Gets the collection of <tt>VirtualServer</tt> instances registered
     * with this <tt>WebContainer</tt>.
     * 
     * @return the (possibly empty) collection of <tt>VirtualServer</tt>
     * instances registered with this <tt>WebContainer</tt>
     */
    public Collection<VirtualServer> getVirtualServers(){
        if (!initialized) {
            init();
        }
        List<VirtualServer> virtualServers = new ArrayList<VirtualServer>();
        for (Container child : engine.findChildren()) {
            if (child instanceof VirtualServer) {
                virtualServers.add((VirtualServer)child);
            }
        }
        return virtualServers;
    }

    /**
     * Stops the given <tt>virtualServer</tt> and removes it from this
     * <tt>WebContainer</tt>.
     *
     * @param virtualServer the <tt>VirtualServer</tt> to be stopped
     * and removed
     *
     * @throws GlassFishException if an error occurs during the stopping
     * or removal of the given <tt>virtualServer</tt>
     */
    public void removeVirtualServer(VirtualServer virtualServer) 
            throws GlassFishException {
        if (!initialized) {
            init();
        }
        engine.removeChild((Container)virtualServer);
    }  
    
    /**
     * Sets the value of the context path
     * 
     * @param path - the path
     */
    public void setPath(File path) {
        this.path = path;
    }
  
    /**
     * Returning the value of the context path
     *
     * @return - the context path
     */
    public File getPath() {
        return path;
    }
    
    /**
     * Sets log level
     * 
     * @param level
     */
    public void setLogLevel(Level level) {
        log.setLevel(level);
    }   
    
}
