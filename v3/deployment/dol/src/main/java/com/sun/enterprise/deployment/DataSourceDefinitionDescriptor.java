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
package com.sun.enterprise.deployment;

import java.util.Properties;
import java.sql.Connection;

/**
 * @author Jagadish Ramu
 */
public class DataSourceDefinitionDescriptor extends Descriptor implements java.io.Serializable {

    private String name ;
    private String description;
    private String className ;
    private int portNumber = -1;
    private String databaseName ;
    private String serverName = "localhost";
    private String url;
    private String user;
    private String password;
    private long loginTimeout =0 ;
    private boolean transactional = true;
    private int isolationLevel = -1;
    private int initialPoolSize =-1;
    private int maxPoolSize = -1;
    private int minPoolSize =-1;
    private long maxIdleTime=-1 ; //seconds / milliseconds ?
    private int maxStatements =-1;
    private Properties properties = new Properties();

    private boolean transactionSet = false;
    private boolean loginTimeoutSet = false; 
    private boolean serverNameSet = false;

    private String componentId;

    private static final String JAVA_URL = "java:";
    private static final String JAVA_COMP_URL = "java:comp/";

    public DataSourceDefinitionDescriptor(){
    }

    public String getComponentId(){
        return componentId;
    }

    public void setComponentId(String componentId){
        this.componentId = componentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String Url){
        this.url = Url;
    }

    public String getUrl(){
        return url;
    }
    public String getDescription() {
        return description;
    }

    public void setTransactionSet(boolean value){
        this.transactionSet = value;
    }

    public boolean isTransactionSet(){
        return transactionSet;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
        setServerNameSet(true);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getLoginTimeout() {
        return loginTimeout;
    }

    public void setLoginTimeout(long loginTimeout) {
        this.loginTimeout = loginTimeout;
        setLoginTimeoutSet(true);
    }

    private void setLoginTimeoutSet(boolean b) {
        loginTimeoutSet = b;
    }

    public boolean isLoginTimeoutSet(){
        return loginTimeoutSet;
    }

    public boolean isServerNameSet(){
        return serverNameSet;
    }

    private void setServerNameSet(boolean b){
        this.serverNameSet = b;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
        setTransactionSet(true);
    }

    public int getIsolationLevel() {
        return isolationLevel;
    }

    public void setIsolationLevel(int isolationLevel) {
        switch(isolationLevel){
            case Connection.TRANSACTION_READ_COMMITTED :
            case Connection.TRANSACTION_READ_UNCOMMITTED :
            case Connection.TRANSACTION_REPEATABLE_READ :
            case Connection.TRANSACTION_SERIALIZABLE :
            this.isolationLevel = isolationLevel;
                break;
            default :
                //TODO V3 log ?
                throw new IllegalStateException
                        ("Isolation level [ "+isolationLevel+" ] not of of standard isolation levels.");
        }
    }

    public int getInitialPoolSize() {
        return initialPoolSize;
    }

    public void setInitialPoolSize(int initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(long maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public int getMaxStatements() {
        return maxStatements;
    }

    public void setMaxStatements(int maxStatements) {
        this.maxStatements = maxStatements;
    }

    public void addProperty(String key, String value){
        properties.put(key, value);
    }
    public String getProperty(String key){
        return (String)properties.get(key);
    }

    public Properties getProperties(){
        return properties;
    }

    public boolean equals(Object object) {
        if (object instanceof DataSourceDefinitionDescriptor) {
            DataSourceDefinitionDescriptor reference = (DataSourceDefinitionDescriptor) object;
            return getJavaName(this.getName()).equals(getJavaName(reference.getName()));
        }
        return false;
    }

    public static String getJavaName(String thisName) {
        if(!thisName.contains(JAVA_URL)){
                thisName = JAVA_COMP_URL + thisName;
        }
        return thisName;
    }

    public void addDataSourcePropertyDescriptor(DataSourcePropertyDescriptor propertyDescriptor){
        properties.put(propertyDescriptor.getName(), propertyDescriptor.getValue());
    }
}
