/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *   Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 *   The contents of this file are subject to the terms of either the GNU
 *   General Public License Version 2 only ("GPL") or the Common Development
 *   and Distribution License("CDDL") (collectively, the "License").  You
 *   may not use this file except in compliance with the License. You can obtain
 *   a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 *   or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 *   language governing permissions and limitations under the License.
 *
 *   When distributing the software, include this License Header Notice in each
 *   file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 *   Sun designates this particular file as subject to the "Classpath" exception
 *   as provided by Sun in the GPL Version 2 section of the License file that
 *   accompanied this code.  If applicable, add the following below the License
 *   Header, with the fields enclosed by brackets [] replaced by your own
 *   identifying information: "Portions Copyrighted [year]
 *   [name of copyright owner]"
 *
 *   Contributor(s):
 *
 *   If you wish your version of this file to be governed by only the CDDL or
 *   only the GPL Version 2, indicate your decision by adding "[Contributor]
 *   elects to include this software in this distribution under the [CDDL or GPL
 *   Version 2] license."  If you don't indicate a single choice of license, a
 *   recipient has the option to distribute your version of this file under
 *   either the CDDL, the GPL Version 2 or to extend the choice of license to
 *   its licensees as provided above.  However, if you add GPL Version 2 code
 *   and therefore, elected the GPL Version 2 license, then the option applies
 *   only if the new code is made subject to such option by the copyright
 *   holder.
 */

package org.glassfish.enterprise.admin.ncli;

import org.glassfish.enterprise.admin.ncli.metadata.CommandDesc;
import org.glassfish.enterprise.admin.ncli.comm.TargetServer;

import java.util.Collections;
import java.util.List;

/** Represents a command class. The goal of the command line parser is to build a command. If the command
 *  can not be built, it is a syntax error. A command comprises of a list of options and a list of operands.
 *  Once a command is built, it is executed on a given server. It is possible that such a command can be executed
 *  locally, but that is not required for this release. Eventually, all commands, local and remote should be
 *  represented by instances of this class.
 *  <p>
 *  &lt;lament> As of now, the world of local and remote commands looks so different! This
 *  difference has to be eliminated. &lt;lament> 
 * <p>
 * Instances of this class are immutable. Since the metadata classes (generated by JAXB) are mutable, this class
 * provides no way to reach the command's metadata or the metadata of the options and operands. Arguably, it can still
 * be said that the state of this command can be modified because the callers can advertently or inadvertently change
 * the metadata passed into the constructor of this class. Callers should be aware of that.
 * <p>
 * This is a package private class.
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @see Option
 * @see Operand
 * @see org.glassfish.enterprise.admin.ncli.metadata.OptionDesc
 * @see CommandDesc
 */
final class NewCommand {
    private final CommandDesc cmdMetadata;
    private final List<Option> options;
    private final List<Operand> operands;

    /** The only constructor that creates an immutable instance of this class.
     *  None of the parameters should be null.
     * @param cmdMetadata CommandDesc representing metadata of the command, may not be null
     * @param options A set of Options of this command, may not be null
     * @param operands A set of Operands of this command, may not be null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    NewCommand(CommandDesc cmdMetadata, List<Option> options, List<Operand> operands) {
        if (cmdMetadata == null || options == null || operands == null)
            throw new IllegalArgumentException("null command metadata, options or operands");
        this.cmdMetadata = cmdMetadata;
        if (cmdMetadata.getName() == null)
            throw new IllegalArgumentException("null name from a non-null metadata"); //should really be an assertion
        this.options     = Collections.unmodifiableList(options);
        this.operands    = Collections.unmodifiableList(operands);
    }

    /** Returns the result of command execution. This method is the essence of this class. It communicates with the
     *  given server and runs this command. Command execution return value, exceptions if any, exit code are communicated
     *  via returned result.
     *
     * @param ts TargetServer instance, may not be null
     * @return result of command execution
     */
    CommandExecutionResult execute(TargetServer ts) {
        if (ts == null)
                throw new IllegalArgumentException("null arg");
        //TODO
        //this is where it creates the request,  communicates with the server and handles the response
        return null;   
    }

    /** Returns the name of the command.
     *
     * @return String representing the name of the command. Never returns a null
     */
    String getName() {
        return cmdMetadata.getName();
    }

    /** Returns an <i> unmodifiable view </i> of the instances of Option that this command has, as specified on
     *  command line. If a user does not specify on command line any of the options that a command has, this set will be empty.
     *  Since the instances of Option returned are immutable, the returned list will never result in change to the state
     *   of this instance of NewCommand.
     * @return an unmodifiable Set of command's options. Never returns a null
     */
    List<Option> getOptions() {
        return options;
    }

    /** Returns an <i> unmodifiable view </i> of command operands, as a list. Since the operands themselves are immutable,
     *  the returned list does not change the state of this instance of NewCommand.
     *
     * @return an unmodifiable list of command's operands. Never returns a null
     */
    List<Operand> getOperands() {
        return operands;
    }

    /** A convenience method that provides a way to get an explicitly specified option with given name. Note that this
     *  method does not deal with the command's metadata, it looks only at explicitly specified option.
     *
     * @param name String representing the <i> name </i> of an option, may not be null
     * @return the Option if specified on command line, null otherwise
     * @throws NullPointerException, if the parameter is null
     */
    Option getExplicitOptionNamed(String name) {
        for(Option opt : options) {
            if (opt.getName().equals(name))
                return opt;
        }
        return null;
    }
}
