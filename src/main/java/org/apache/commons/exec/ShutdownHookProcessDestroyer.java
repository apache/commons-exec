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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Destroys all registered {@code Process}es when the VM exits.
 */
public class ShutdownHookProcessDestroyer implements ProcessDestroyer, Runnable {

    private final class ProcessDestroyerThread extends Thread {

        /**
         * Whether to run the ShutdownHookProcessDestroyer.
         */
        private final AtomicBoolean shouldDestroy = new AtomicBoolean(true);

        private ProcessDestroyerThread() {
            super("ProcessDestroyer Shutdown Hook");
        }

        @Override
        public void run() {
            if (shouldDestroy.get()) {
                ShutdownHookProcessDestroyer.this.run();
            }
        }

        public void setShouldDestroy(final boolean shouldDestroy) {
            this.shouldDestroy.compareAndSet(!shouldDestroy, shouldDestroy);
        }
    }

    /** The list of currently running processes. */
    private final List<Process> processes = new ArrayList<>();

    /** The thread registered at the JVM to execute the shutdown handler. */
    private ProcessDestroyerThread destroyProcessThread;

    /** Whether or not this ProcessDestroyer has been registered as a shutdown hook. */
    private final AtomicBoolean added = new AtomicBoolean();

    /**
     * Whether or not this ProcessDestroyer is currently running as shutdown hook.
     */
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * Constructs a {@code ProcessDestroyer} and obtains {@code Runtime.addShutdownHook()} and {@code Runtime.removeShutdownHook()} through reflection. The
     * ProcessDestroyer manages a list of processes to be destroyed when the VM exits. If a process is added when the list is empty, this
     * {@code ProcessDestroyer} is registered as a shutdown hook. If removing a process results in an empty list, the {@code ProcessDestroyer} is removed as a
     * shutdown hook.
     */
    public ShutdownHookProcessDestroyer() {
    }

    /**
     * Returns {@code true} if the specified {@code Process} was successfully added to the list of processes to destroy upon VM exit.
     *
     * @param process the process to add.
     * @return {@code true} if the specified {@code Process} was successfully added.
     */
    @Override
    public boolean add(final Process process) {
        synchronized (processes) {
            // if this list is empty, register the shutdown hook
            if (processes.isEmpty()) {
                addShutdownHook();
            }
            processes.add(process);
            return processes.contains(process);
        }
    }

    /**
     * Registers this {@code ProcessDestroyer} as a shutdown hook.
     */
    private void addShutdownHook() {
        if (!running.get()) {
            destroyProcessThread = new ProcessDestroyerThread();
            Runtime.getRuntime().addShutdownHook(destroyProcessThread);
            added.compareAndSet(false, true);
        }
    }

    /**
     * Tests whether or not the ProcessDestroyer is registered as shutdown hook.
     *
     * @return true if this is currently added as shutdown hook.
     */
    public boolean isAddedAsShutdownHook() {
        return added.get();
    }

    /**
     * Tests emptiness (size == 0).
     *
     * @return Whether or not this is empty.
     * @since 1.4.0
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns {@code true} if the specified {@code Process} was successfully removed from the list of processes to destroy upon VM exit.
     *
     * @param process the process to remove.
     * @return {@code true} if the specified {@code Process} was successfully removed.
     */
    @Override
    public boolean remove(final Process process) {
        synchronized (processes) {
            final boolean processRemoved = processes.remove(process);
            if (processRemoved && processes.isEmpty()) {
                removeShutdownHook();
            }
            return processRemoved;
        }
    }

    /**
     * Removes this {@code ProcessDestroyer} as a shutdown hook.
     */
    private void removeShutdownHook() {
        if (added.get() && !running.get()) {
            final boolean removed = Runtime.getRuntime().removeShutdownHook(destroyProcessThread);
            if (!removed) {
                System.err.println("Could not remove shutdown hook");
            }
            // start the hook thread, a unstarted thread may not be eligible for garbage collection Cf.: https://developer.java.sun.com/developer/
            // bugParade/bugs/4533087.html
            destroyProcessThread.setShouldDestroy(false);
            destroyProcessThread.start();
            // this should return quickly, since it basically is a NO-OP.
            try {
                destroyProcessThread.join(20000);
            } catch (final InterruptedException ignore) {
                // the thread didn't die in time
                // it should not kill any processes unexpectedly
            }
            destroyProcessThread = null;
            added.compareAndSet(true, false);
        }
    }

    /**
     * Invoked by the VM when it is exiting.
     */
    @Override
    public void run() {
        synchronized (processes) {
            running.compareAndSet(false, true);
            processes.forEach(process -> {
                try {
                    process.destroy();
                } catch (final Throwable t) {
                    System.err.println("Unable to terminate process during process shutdown");
                }
            });
        }
    }

    /**
     * Returns the number of registered processes.
     *
     * @return the number of register process.
     */
    @Override
    public int size() {
        return processes.size();
    }
}
