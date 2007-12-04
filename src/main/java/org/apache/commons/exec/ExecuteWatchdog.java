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

import java.io.IOException;

/**
 * Destroys a process running for too long. For example:
 * 
 * <pre>
 * ExecuteWatchdog watchdog = new ExecuteWatchdog(30000);
 * Execute exec = new Execute(myloghandler, watchdog);
 * exec.setCommandLine(mycmdline);
 * int exitvalue = exec.execute();
 * if (Execute.isFailure(exitvalue) &amp;&amp; watchdog.killedProcess()) {
 *     // it was killed on purpose by the watchdog
 * }
 * </pre>
 * 
 * @see org.apache.commons.exec.Executor
 * @see org.apache.commons.exec.Watchdog
 */
public class ExecuteWatchdog implements TimeoutObserver {

    /** The process to execute and watch for duration. */
    private Process process;

    /** Say whether or not the watchdog is currently monitoring a process. */
    private boolean watch;

    /** Exception that might be thrown during the process execution. */
    private Exception caught;

    /** Say whether or not the process was killed due to running overtime. */
    private boolean killedProcess;

    /** Will tell us whether timeout has occurred. */
    private Watchdog watchdog;

    /**
     * Creates a new watchdog with a given timeout.
     * 
     * @param timeout
     *            the timeout for the process in milliseconds. It must be
     *            greater than 0.
     */
    public ExecuteWatchdog(final long timeout) {
        this.killedProcess = false;
        this.watch = false;
        this.watchdog = new Watchdog(timeout);
        this.watchdog.addTimeoutObserver(this);
    }

    /**
     * Watches the given process and terminates it, if it runs for too long. All
     * information from the previous run are reset.
     * 
     * @param process
     *            the process to monitor. It cannot be <tt>null</tt>
     * @throws IllegalStateException
     *             if a process is still being monitored.
     */
    public synchronized void start(final Process process) {
        if (process == null) {
            throw new NullPointerException("process is null.");
        }
        if (this.process != null) {
            throw new IllegalStateException("Already running.");
        }
        this.caught = null;
        this.killedProcess = false;
        this.watch = true;
        this.process = process;
        watchdog.start();
    }

    /**
     * Stops the watcher. It will notify all threads possibly waiting on this
     * object.
     */
    public synchronized void stop() {
        watchdog.stop();
        watch = false;
        process = null;
    }

    /**
     * Destroys the running process manually.
     */
    public synchronized void destroyProcess() {
        this.timeoutOccured(new Watchdog(1));
        this.stop();
    }

    /**
     * Called after watchdog has finished.
     */
    public synchronized void timeoutOccured(final Watchdog w) {
        try {
            try {
                // We must check if the process was not stopped
                // before being here
                if(process != null) {
                    process.exitValue();
                }
            } catch (IllegalThreadStateException itse) {
                // the process is not terminated, if this is really
                // a timeout and not a manual stop then destroy it.
                if (watch) {
                    killedProcess = true;
                    process.destroy();
                }
            }
        } catch (Exception e) {
            caught = e;
        } finally {
            cleanUp();
        }
    }


    /**
     * This method will rethrow the exception that was possibly caught during
     * the run of the process. It will only remains valid once the process has
     * been terminated either by 'error', timeout or manual intervention.
     * Information will be discarded once a new process is ran.
     * 
     * @throws IOException
     *             a wrapped exception over the one that was silently swallowed
     *             and stored during the process run.
     */
    public synchronized void checkException() throws Exception {
        if (caught != null) {
            throw caught;
        }
    }

    /**
     * Indicates whether or not the watchdog is still monitoring the process.
     * 
     * @return <tt>true</tt> if the process is still running, otherwise
     *         <tt>false</tt>.
     */
    public synchronized boolean isWatching() {
        return watch;
    }

    /**
     * Indicates whether the last process run was killed.
     * 
     * @return <tt>true</tt> if the process was killed
     *         <tt>false</tt>.
     */
    public synchronized boolean killedProcess() {
        return killedProcess;
    }

    /**
     * reset the monitor flag and the process.
     */
    protected void cleanUp() {
        watch = false;
        process = null;
    }    
}
