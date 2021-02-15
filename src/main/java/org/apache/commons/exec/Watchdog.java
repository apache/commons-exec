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

import java.util.Enumeration;
import java.util.Vector;

/**
 * Generalization of {@code ExecuteWatchdog}
 *
 * @see org.apache.commons.exec.ExecuteWatchdog
 *
 */
public class Watchdog implements Runnable {

    private final Vector<TimeoutObserver> observers = new Vector<>(1);

    private final long timeoutMillis;

    private boolean stopped;

    public Watchdog(final long timeoutMillis) {
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeout must not be less than 1.");
        }
        this.timeoutMillis = timeoutMillis;
    }

    public void addTimeoutObserver(final TimeoutObserver to) {
        observers.addElement(to);
    }

    public void removeTimeoutObserver(final TimeoutObserver to) {
        observers.removeElement(to);
    }

    protected final void fireTimeoutOccured() {
        final Enumeration<TimeoutObserver> e = observers.elements();
        while (e.hasMoreElements()) {
            e.nextElement().timeoutOccured(this);
        }
    }

    public synchronized void start() {
        stopped = false;
        final Thread t = new Thread(this, "WATCHDOG");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void stop() {
        stopped = true;
        notifyAll();
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
                } catch (final InterruptedException e) {
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

}
