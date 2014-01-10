/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.exec.issues;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.TestUtil;
import org.junit.Test;

/**
 * Test EXEC-60 (https://issues.apache.org/jira/browse/EXEC-60).
 *
 * @version $Id$
 */
public class Exec60Test {

    private final Executor exec = new DefaultExecutor();
    private final File testDir = new File("src/test/scripts");
    private final File pingScript = TestUtil.resolveScriptForOS(testDir + "/ping");

    /**
     * Possible deadlock when a process is terminating at the same time its timing out. Please
     * note that a successful test is no proof that the issues was indeed fixed.
     */
    @Test
    public void testExec_60() throws Exception {

        final int start = 0;
        final int seconds = 1;
        final int offsetMultiplier = 1;
        final int maxRetries = 180;
        int processTerminatedCounter = 0;
        int watchdogKilledProcessCounter = 0;
        final CommandLine cmdLine = new CommandLine(pingScript);
        cmdLine.addArgument(Integer.toString(seconds + 1)); // need to add "1" to wait the requested number of seconds

        final long startTime = System.currentTimeMillis();
        for (int offset = start; offset <= maxRetries; offset++) {
            // wait progressively longer for process to complete
            // tricky to get this test right. We want to try and catch the process while it is terminating,
            // so we increase the timeout gradually until the test terminates normally.
            // However if the increase is too gradual, we never wait long enough for any test to exit normally
            final ExecuteWatchdog watchdog = new ExecuteWatchdog(seconds * 1000 + offset * offsetMultiplier);
            exec.setWatchdog(watchdog);
            try {
                exec.execute(cmdLine);
                processTerminatedCounter++;
//                System.out.println(offset + ": process has terminated: " + watchdog.killedProcess());
                if (processTerminatedCounter > 5) {
                    break;
                }
            } catch (final ExecuteException ex) {
//                System.out.println(offset + ": process was killed: " + watchdog.killedProcess());
                assertTrue("Watchdog killed the process", watchdog.killedProcess());
                watchdogKilledProcessCounter++;
            }
        }

        final long avg = (System.currentTimeMillis() - startTime) /
                (watchdogKilledProcessCounter+processTerminatedCounter);
        System.out.println("Processes terminated: " + processTerminatedCounter + " killed: " + watchdogKilledProcessCounter
                + " Multiplier: " + offsetMultiplier + " MaxRetries: " + maxRetries + " Elapsed (avg ms): " + avg);
        assertTrue("Not a single process terminated on its own", processTerminatedCounter > 0);
        assertTrue("Not a single process was killed by the watch dog", watchdogKilledProcessCounter > 0);
    }
}
