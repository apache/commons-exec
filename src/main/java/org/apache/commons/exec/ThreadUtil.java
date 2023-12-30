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

import java.util.concurrent.ThreadFactory;

/**
 * Internal thread helper.
 */
final class ThreadUtil {

    /**
     * Creates a new Thread from the given factory and prefixes it's name with a prefix and sets the daemon flag.
     *
     * @param threadFactory the thread factory.
     * @param runnable      The runnable to thread.
     * @param prefix        the thread name prefix
     * @param daemon        marks this thread as a daemon thread
     * @return constructed thread, or {@code null} if the request to create a thread is rejected
     */
    static Thread newThread(final ThreadFactory threadFactory, final Runnable runnable, final String prefix, final boolean daemon) {
        final Thread thread = threadFactory.newThread(runnable);
        if (thread == null) {
            throw new IllegalStateException(String.format("The ThreadFactory %s cound not construct a thread for '%s'", threadFactory, prefix));
        }
        thread.setName(prefix + thread.getName());
        thread.setDaemon(daemon);
        return thread;
    }

}
