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

    /** The exit value of the finished process */
    private int exitValue;

    /** Any offending exception */
    private ExecuteException exception;

    /**
     * @see org.apache.commons.exec.ExecuteResultHandler#onProcessComplete(int)
     */
    public void onProcessComplete(int exitValue) {
        this.exitValue = exitValue;
    }

    /**
     * @see org.apache.commons.exec.ExecuteResultHandler#onProcessFailed(org.apache.commons.exec.ExecuteException)
     */
    public void onProcessFailed(ExecuteException e) {
        this.exception = e;
        exitValue = e.getExitValue();
    }

    /**
     * @return Returns the exception.
     */
    public ExecuteException getException() {
        return exception;
    }

    /**
     * @return Returns the exitValue.
     */
    public int getExitValue() {
        return exitValue;
    }
}