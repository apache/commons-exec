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

/**
 * Destroys all registered {@link Process} after a certain event, typically when the VM exits.
 *
 * @see ShutdownHookProcessDestroyer
 */
public interface ProcessDestroyer {

    /**
     * Returns {@code true} if the specified {@link Process} was successfully added to the list of processes to be destroy.
     *
     * @param process the process to add.
     * @return {@code true} if the specified {@link Process} was successfully added.
     */
    boolean add(Process process);

    /**
     * Returns {@code true} if the specified {@link Process} was successfully removed from the list of processes to be destroy.
     *
     * @param process the process to remove.
     * @return {@code true} if the specified {@link Process} was successfully removed.
     */
    boolean remove(Process process);

    /**
     * Returns the number of registered processes.
     *
     * @return the number of registered processes.
     */
    int size();
}
