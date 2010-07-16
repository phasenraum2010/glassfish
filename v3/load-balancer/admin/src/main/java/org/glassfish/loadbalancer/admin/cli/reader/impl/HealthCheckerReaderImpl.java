/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.loadbalancer.admin.cli.reader.impl;

import org.glassfish.loadbalancer.admin.cli.transform.Visitor;
import org.glassfish.loadbalancer.admin.cli.transform.HealthCheckerVisitor;
import org.glassfish.loadbalancer.admin.cli.reader.api.HealthCheckerReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.LbReaderException;
import com.sun.enterprise.config.serverbeans.HealthChecker;
import org.glassfish.loadbalancer.admin.cli.LbLogUtil;

/**
 * Provides health checker information relavant to Load balancer tier.
 *
 * @author Kshitiz Saxena
 */
public class HealthCheckerReaderImpl implements HealthCheckerReader {

    static HealthCheckerReader getDefaultHealthChecker() {
        return defaultHCR;
    }

    public HealthCheckerReaderImpl() {
        _hc = null;
    }

    /**
     * Constructor 
     */
    public HealthCheckerReaderImpl(HealthChecker hc) {
        if (hc == null) {
            String msg = LbLogUtil.getStringManager().getString("ConfigBeanAndNameNull");
            throw new IllegalArgumentException(msg);
        }
        _hc = hc;
    }

    /**
     * Return health checker url
     *
     * @return String           health checker url, it shoudld conform to
     *                          RFC 2396. java.net.URI.resolve(url) shoudl
     *                          return a valid URI.
     */
    @Override
    public String getUrl() throws LbReaderException {
        if (_hc == null) {
            return defaultURL;
        }
        return _hc.getUrl();
    }

    /**
     * Health checker runs in the specified interval time.
     *
     * @return String           value must be > 0
     */
    @Override
    public String getIntervalInSeconds() throws LbReaderException {
        if (_hc == null) {
            return defaultInterval;
        }
        return _hc.getIntervalInSeconds();
    }

    /**
     *  Timeout where a server is considered un healthy.
     *
     * @return String           value must be > 0
     */
    @Override
    public String getTimeoutInSeconds() throws LbReaderException {
        if (_hc == null) {
            return defaultTimeout;
        }
        return _hc.getTimeoutInSeconds();
    }

    // --- VISITOR IMPLEMENTATION ---
    @Override
    public void accept(Visitor v) throws Exception {

        HealthCheckerVisitor pv = (HealthCheckerVisitor) v;
        pv.visit(this);
    }
    //--- PRIVATE VARIABLES ------
    HealthChecker _hc = null;
    private static final HealthCheckerReader defaultHCR =
            new HealthCheckerReaderImpl();
    private static final String defaultURL = "/";
    private static final String defaultInterval = "10";
    private static final String defaultTimeout = "30";
}
