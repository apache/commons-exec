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
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.TestUtil;
import org.junit.Test;

/**
 * Test the patch for EXEC-41 (https://issues.apache.org/jira/browse/EXEC-41).
 *
 * @version $Id$
 */
public class Exec41Test {

    private final File testDir = new File("src/test/scripts");
    private final File pingScript = TestUtil.resolveScriptForOS(testDir + "/ping");

    /**
     *
     * When a process runs longer than allowed by a configured watchdog's
     * timeout, the watchdog tries to destroy it and then DefaultExecutor
     * tries to clean up by joining with all installed pump stream threads.
     * Problem is, that sometimes the native process doesn't die and thus
     * streams aren't closed and the stream threads do not complete.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec41WithStreams() throws Exception {

        CommandLine cmdLine;

        if (OS.isFamilyWindows()) {
            cmdLine = CommandLine.parse("ping.exe -n 10 -w 1000 127.0.0.1");
        } else if ("HP-UX".equals(System.getProperty("os.name"))) {
            // see EXEC-52 - option must appear after the hostname!
            cmdLine = CommandLine.parse("ping 127.0.0.1 -n 10");
        } else if (OS.isFamilyUnix()) {
            cmdLine = CommandLine.parse("ping -c 10 127.0.0.1");
        } else {
            System.err.println("The test 'testExec41WithStreams' does not support the following OS : "
                    + System.getProperty("os.name"));
            return;
        }

        final DefaultExecutor executor = new DefaultExecutor();
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(2 * 1000); // allow process no more than 2 secs
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(System.out, System.err);
        // this method was part of the patch I reverted
        // pumpStreamHandler.setAlwaysWaitForStreamThreads(false);

        executor.setWatchdog(watchdog);
        executor.setStreamHandler(pumpStreamHandler);

        final long startTime = System.currentTimeMillis();

        try {
            executor.execute(cmdLine);
        } catch (final ExecuteException e) {
            // nothing to do
        }

        final long duration = System.currentTimeMillis() - startTime;

        System.out.println("Process completed in " + duration + " millis; below is its output");

        if (watchdog.killedProcess()) {
            System.out.println("Process timed out and was killed by watchdog.");
        }

        assertTrue("The process was killed by the watchdog", watchdog.killedProcess());
        assertTrue("Skipping the Thread.join() did not work", duration < 9000);
    }

    /**
     * Test EXEC-41 with a disabled PumpStreamHandler to check if we could return
     * immediately after killing the process (no streams implies no blocking
     * stream pumper threads). But you have to be 100% sure that the subprocess
     * is not writing to 'stdout' and 'stderr'.
     *
     * For this test we are using the batch file - under Windows the 'ping'
     * process can't be killed (not supported by Win32) and will happily
     * run the given time (e.g. 10 seconds) even hwen the batch file is already
     * killed.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec41WithoutStreams() throws Exception {

        final CommandLine cmdLine = new CommandLine(pingScript);
        cmdLine.addArgument("10"); // sleep 10 secs
        final DefaultExecutor executor = new DefaultExecutor();
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(2*1000); // allow process no more than 2 secs

        // create a custom "PumpStreamHandler" doing no pumping at all
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(null, null, null);

        executor.setWatchdog(watchdog);
        executor.setStreamHandler(pumpStreamHandler);

        final long startTime = System.currentTimeMillis();

        try {
            executor.execute(cmdLine);
        } catch (final ExecuteException e) {
            System.out.println(e);
        }

        final long duration = System.currentTimeMillis() - startTime;

        System.out.println("Process completed in " + duration +" millis; below is its output");

        if (watchdog.killedProcess()) {
            System.out.println("Process timed out and was killed.");
        }

        assertTrue("The process was killed by the watchdog", watchdog.killedProcess());
        assertTrue("Skipping the Thread.join() did not work, duration="+duration, duration < 9000);
    }
}
