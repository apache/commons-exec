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

    private static final int SLEEP_TIME_MS = 100;

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
     * Get the <code>exception<code> causing the process execution to fail.
     *
     * @return Returns the exception.
     * @throws IllegalStateException if the process has not exited yet
     */
    synchronized public ExecuteException getException() {
        if(!hasResult) throw new IllegalStateException("The process has not exited yet therefore no result is available ...");
        return exception;
    }

    /**
     * Get the <code>exitValue<code> of the process.
     *
     * @return Returns the exitValue.
     * @throws IllegalStateException if the process has not exited yet
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

    /**
     * Causes the current thread to wait, if necessary, until the
     * process has terminated. This method returns immediately if
     * the process has already terminated. If the process has
     * not yet terminated, the calling thread will be blocked until the
     * process exits.
     *
     * @return     the exit value of the process.
     * @exception  InterruptedException if the current thread is
     *             {@linkplain Thread#interrupt() interrupted} by another
     *             thread while it is waiting, then the wait is ended and
     *             an {@link InterruptedException} is thrown.
     * @exception  ExecuteException re-thrown exception if the process 
     *             execution has failed due to ExecuteException
     */
    public int waitFor() throws InterruptedException, ExecuteException {
        while (!this.hasResult()) {
            Thread.sleep(SLEEP_TIME_MS);
        }

        if(getException() == null) {
            return getExitValue();
        }
        else {
            throw getException();
        }
    }
}