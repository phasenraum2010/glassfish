/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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

package org.glassfish.api.admin;

import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.*;
import org.jvnet.hk2.annotations.Contract;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * CommandRunner is a service that allow to run administrative commands.
 *
 * @author Jerome Dochez
 */
@Contract
public interface CommandRunner {

    /**
     * Returns an initialized ActionReport instance for the passed type or
     * null if it cannot be found.
     *
     * @param name actiopn report type name
     * @return uninitialized action report or null
     */
    public ActionReport getActionReport(String name);

    /**
     * Retuns the command model for a command name
     *
     * @param name command name
     * @param logger logger to log any error messages
     * @return model for this command (list of parameters,etc...), null if command
     * is not found
     */
    public CommandModel getModel(String name, Logger logger);

    /**
     * Obtain and return the command implementation defined by the passed commandName
     *
     * @param commandName command name as typed by users
     * @param report report used to communicate command status back to the user
     * @param logger logger to log
     * @return command registered under commandName or null if not found.
     */
    public AdminCommand getCommand(String commandName, ActionReport report, Logger logger);

    /**
     * Obtain a new command invocation object. Command invocations can be configured and used
     * to trigger a command execution.
     *
     * @param name name of the requested command to invoke
     * @param report where to place the status of the command execution
     * @return a new command invocation for that command name.
     */
    CommandInvocation getCommandInvocation(String name, ActionReport report);


    /**
     * CommandInvocation defines a command excecution context like the requested
     * name of the command to execute, the parameters of the command, etc...
     * 
     */
    public interface CommandInvocation {

        /**
         * Sets the command parameters as a typed inteface
         * @param opsParams parameters
         * @return itself
         */
        CommandInvocation parameters(OpsParams opsParams);

        /**
         * Sets the command parameters as a properties bag
         * @param params the parameters
         * @return itself
         */
        CommandInvocation parameters(Properties params);

        /**
         * Sets the data carried with the request (could be an attachment) 
         * @param inbound inbound data
         * @return itself
         */
        CommandInvocation inbound(Payload.Inbound inbound);

        /**
         * Sets the data carried with the response
         * @param outbound outbound data
         * @return itself
         */
        CommandInvocation outbound(Payload.Outbound outbound);

        /**
         * Executes the command and populate the report with the command
         * execution result. Parameters must have been set before invoking
         * this method.
         */
        void execute();

        /**
         * Executes the passed command with this context and populates the
         * report with the execution result. Parameters must be set before
         * invoking this command.
         *
         * @param command command implementation to execute
         */
        void execute(AdminCommand command);

    }
}
