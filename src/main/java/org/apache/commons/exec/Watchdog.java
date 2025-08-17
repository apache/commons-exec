/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.exec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

/**
 * Generalization of {@code ExecuteWatchdog}.
 *
 * @see org.apache.commons.exec.ExecuteWatchdog
 */
public class Watchdog implements Runnable {

    /**
     * Builds ExecuteWatchdog instances.
     *
     * @since 1.4.0
     */
    public static final class Builder implements Supplier<Watchdog> {

        private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

        /** Thread factory. */
        private ThreadFactory threadFactory;

        /**
         * Timeout duration.
         */
        private Duration timeout = DEFAULT_TIMEOUT;

        /**
         * Constructs a new instance.
         */
        public Builder() {
            // empty
        }

        /**
         * Creates a new configured ExecuteWatchdog.
         *
         * @return a new configured ExecuteWatchdog.
         */
        @Override
        public Watchdog get() {
            return new Watchdog(this);
        }

        /**
         * Sets the thread factory.
         *
         * @param threadFactory the thread factory.
         * @return {@code this} instance.
         */
        public Builder setThreadFactory(final ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        /**
         * Sets the timeout duration.
         *
         * @param timeout the timeout duration, null resets to the default 30 seconds.
         * @return {@code this} instance.
         */
        public Builder setTimeout(final Duration timeout) {
            this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
            return this;
        }

    }

    /**
     * Creates a new builder.
     *
     * @return a new builder.
     * @since 1.4.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Observers.
     */
    private final List<TimeoutObserver> observers = new ArrayList<>(1);

    /**
     * Timeout duration.
     */
    private final long timeoutMillis;

    /**
     * Whether this is stopped.
     */
    private boolean stopped;

    /**
     * The thread factory.
     */
    private final ThreadFactory threadFactory;

    /**
     * Constructs a new instance.
     *
     * @param timeoutMillis the timeout duration.
     * @deprecated Use {@link Builder#get()}.
     */
    @Deprecated
    public Watchdog(final long timeoutMillis) {
        this(builder().setTimeout(Duration.ofMillis(timeoutMillis)));
    }

    /**
     * Constructs a new instance.
     *
     * @param threadFactory the thread factory.
     * @param timeout       the timeout duration.
     */
    private Watchdog(final Builder builder) {
        if (builder.timeout.isNegative() || Duration.ZERO.equals(builder.timeout)) {
            throw new IllegalArgumentException("Timeout must be positive.");
        }
        this.timeoutMillis = builder.timeout.toMillis();
        this.threadFactory = builder.threadFactory;
    }

    /**
     * Adds a TimeoutObserver.
     *
     * @param to a TimeoutObserver to add.
     */
    public void addTimeoutObserver(final TimeoutObserver to) {
        observers.add(to);
    }

    /**
     * Fires a timeout occurred event for each observer.
     */
    protected final void fireTimeoutOccured() {
        observers.forEach(o -> o.timeoutOccured(this));
    }

    /**
     * Removes a TimeoutObserver.
     *
     * @param to a TimeoutObserver to remove.
     */
    public void removeTimeoutObserver(final TimeoutObserver to) {
        observers.remove(to);
    }

    @Override
    public void run() {
        final long startTimeMillis = System.currentTimeMillis();
        boolean isWaiting;
        synchronized (this) {
            long timeLeftMillis = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);
            isWaiting = timeLeftMillis > 0;
            while (!stopped && isWaiting) {
                try {
                    wait(timeLeftMillis);
                } catch (final InterruptedException ignore) {
                    // ignore
                }
                timeLeftMillis = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);
                isWaiting = timeLeftMillis > 0;
            }
        }
        // notify the listeners outside of the synchronized block (see EXEC-60)
        if (!isWaiting) {
            fireTimeoutOccured();
        }
    }

    /**
     * Starts a new thread.
     */
    public synchronized void start() {
        stopped = false;
        ThreadUtil.newThread(threadFactory, this, "CommonsExecWatchdog-", true).start();
    }

    /**
     * Requests a thread stop.
     */
    public synchronized void stop() {
        stopped = true;
        notifyAll();
    }

}
