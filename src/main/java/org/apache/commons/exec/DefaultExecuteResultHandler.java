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
 */

package org.apache.commons.exec;

import java.time.Duration;
import java.time.Instant;

/**
 * A default implementation of 'ExecuteResultHandler' used for asynchronous process handling.
 */
public class DefaultExecuteResultHandler implements ExecuteResultHandler {

    /** The interval polling the result. */
    private static final int SLEEP_TIME_MS = 50;

    /** Keep track if the process is still running. */
    private volatile boolean hasResult;

    /** The exit value of the finished process. */
    private volatile int exitValue;

    /** Any offending exception. */
    private volatile ExecuteException exception;

    /**
     * Constructs a new instance.
     */
    public DefaultExecuteResultHandler() {
        this.hasResult = false;
        this.exitValue = Executor.INVALID_EXITVALUE;
    }

    /**
     * Gets the {@code exception} causing the process execution to fail.
     *
     * @return the exception.
     * @throws IllegalStateException if the process has not exited yet.
     */
    public ExecuteException getException() {
        if (!hasResult) {
            throw new IllegalStateException("The process has not exited yet therefore no result is available ...");
        }
        return exception;
    }

    /**
     * Gets the {@code exitValue} of the process.
     *
     * @return the exitValue.
     * @throws IllegalStateException if the process has not exited yet.
     */
    public int getExitValue() {
        if (!hasResult) {
            throw new IllegalStateException("The process has not exited yet therefore no result is available ...");
        }
        return exitValue;
    }

    /**
     * Tests whether the process exited and a result is available, i.e. exitCode or exception?
     *
     * @return true whether a result of the execution is available.
     */
    public boolean hasResult() {
        return hasResult;
    }

    /**
     * @see org.apache.commons.exec.ExecuteResultHandler#onProcessComplete(int)
     */
    @Override
    public void onProcessComplete(final int exitValue) {
        this.exitValue = exitValue;
        this.exception = null;
        this.hasResult = true;
    }

    /**
     * @see org.apache.commons.exec.ExecuteResultHandler#onProcessFailed(org.apache.commons.exec.ExecuteException)
     */
    @Override
    public void onProcessFailed(final ExecuteException e) {
        this.exitValue = e.getExitValue();
        this.exception = e;
        this.hasResult = true;
    }

    /**
     * Causes the current thread to wait, if necessary, until the process has terminated. This method returns immediately if the process has already terminated.
     * If the process has not yet terminated, the calling thread will be blocked until the process exits.
     *
     * @throws InterruptedException if the current thread is {@linkplain Thread#interrupt() interrupted} by another thread while it is waiting, then the wait is
     *                              ended and an {@link InterruptedException} is thrown.
     */
    public void waitFor() throws InterruptedException {
        while (!hasResult()) {
            Thread.sleep(SLEEP_TIME_MS);
        }
    }

    /**
     * Causes the current thread to wait, if necessary, until the process has terminated. This method returns immediately if the process has already terminated.
     * If the process has not yet terminated, the calling thread will be blocked until the process exits.
     *
     * @param timeout the maximum time to wait.
     * @throws InterruptedException if the current thread is {@linkplain Thread#interrupt() interrupted} by another thread while it is waiting, then the wait is
     *                              ended and an {@link InterruptedException} is thrown.
     * @since 1.4.0
     */
    public void waitFor(final Duration timeout) throws InterruptedException {
        final Instant until = Instant.now().plus(timeout);
        while (!hasResult() && Instant.now().isBefore(until)) {
            Thread.sleep(SLEEP_TIME_MS);
        }
    }

    /**
     * Causes the current thread to wait, if necessary, until the process has terminated. This method returns immediately if the process has already terminated.
     * If the process has not yet terminated, the calling thread will be blocked until the process exits.
     *
     * @param timeoutMillis the maximum time to wait in milliseconds.
     * @throws InterruptedException if the current thread is {@linkplain Thread#interrupt() interrupted} by another thread while it is waiting, then the wait is
     *                              ended and an {@link InterruptedException} is thrown.
     * @deprecated Use {@link #waitFor(Duration)}.
     */
    @Deprecated
    public void waitFor(final long timeoutMillis) throws InterruptedException {
        final long untilMillis = System.currentTimeMillis() + timeoutMillis;
        while (!hasResult() && System.currentTimeMillis() < untilMillis) {
            Thread.sleep(SLEEP_TIME_MS);
        }
    }

}