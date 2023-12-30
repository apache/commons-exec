/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.exec;

import java.io.File;
import java.util.concurrent.ThreadFactory;

/**
 * Runs daemon processes asynchronously. Callers are expected to register a {@link ProcessDestroyer} before executing any processes.
 *
 * @since 1.3
 */
public class DaemonExecutor extends DefaultExecutor {

    /**
     * Constructs a new builder.
     *
     * @since 1.4.0
     */
    public static class Builder extends DefaultExecutor.Builder<Builder> {

        /**
         * Creates a new configured DaemonExecutor.
         *
         * @return a new configured DaemonExecutor.
         */
        @Override
        public DefaultExecutor get() {
            return new DaemonExecutor(getThreadFactory(), getExecuteStreamHandler(), getWorkingDirectory());
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
     * Constructs a new instance.
     *
     * @deprecated Use {@link Builder#get()}.
     */
    @Deprecated
    public DaemonExecutor() {
        // super
    }

    private DaemonExecutor(final ThreadFactory threadFactory, final ExecuteStreamHandler executeStreamHandler, final File workingDirectory) {
        super(threadFactory, executeStreamHandler, workingDirectory);
    }

    /**
     * Factory method to create a thread waiting for the result of an asynchronous execution.
     *
     * @param runnable the runnable passed to the thread.
     * @param name     the name of the thread.
     * @return the thread.
     */
    @Override
    protected Thread createThread(final Runnable runnable, final String name) {
        final Thread thread = super.createThread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }
}
