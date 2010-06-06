/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.exec;

/**
 * A default implementation of 'ExecuteResultHandler' used for asynchronous
 * process handling.
 */
public class DefaultExecuteResultHandler implements ExecuteResultHandler {

    /** Keep track if the process is still running */
    private boolean hasResult;

    /** The exit value of the finished process */
    private int exitValue;

    /** Any offending exception */
    private ExecuteException exception;

    /**
     * @see org.apache.commons.exec.ExecuteResultHandler#onProcessComplete(int)
     */
    synchronized public void onProcessComplete(int exitValue) {
        this.exitValue = exitValue;
        this.hasResult = true;
    }

    /**
     * @see org.apache.commons.exec.ExecuteResultHandler#onProcessFailed(org.apache.commons.exec.ExecuteException)
     */
    synchronized public void onProcessFailed(ExecuteException e) {
        this.exception = e;
        exitValue = e.getExitValue();
        this.hasResult = true;
    }

    /**
     * @return Returns the exception.
     */
    synchronized public ExecuteException getException() {
        if(!hasResult) throw new IllegalStateException("The process has not exited yet therefore no result is available ...");
        return exception;
    }

    /**
     * @return Returns the exitValue.
     */
    synchronized public int getExitValue() {
        if(!hasResult) throw new IllegalStateException("The process has not exited yet therefore no result is available ...");
        return exitValue;
    }

    /**
     * Has the process exited and a result is available?
     *
     * @return true if a result of the execution is available
     */
    synchronized public boolean hasResult() {
        return hasResult;
    }
}