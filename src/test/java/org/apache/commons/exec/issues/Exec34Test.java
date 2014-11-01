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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.TestUtil;
import org.junit.Test;

/**
 * EXEC-34 https://issues.apache.org/jira/browse/EXEC-34
 *
 * @version $Id$
 */
public class Exec34Test {

    private final Executor exec = new DefaultExecutor();
    private final File testDir = new File("src/test/scripts");
    private final File pingScript = TestUtil.resolveScriptForOS(testDir + "/ping");

    /**
     *
     * Race condition prevent watchdog working using ExecuteStreamHandler.
     * The test fails because when watchdog.destroyProcess() is invoked the
     * external process is not bound to the watchdog yet.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec34_1() throws Exception {

        final CommandLine cmdLine = new CommandLine(pingScript);
        cmdLine.addArgument("10"); // sleep 10 secs

        final ExecuteWatchdog watchdog = new ExecuteWatchdog(Integer.MAX_VALUE);
        final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        exec.setWatchdog(watchdog);
        exec.execute(cmdLine, handler);
        assertTrue(watchdog.isWatching());
        watchdog.destroyProcess();
        assertTrue("Watchdog should have killed the process",watchdog.killedProcess());
        assertFalse("Watchdog is no longer watching the process",watchdog.isWatching());
    }

    /**
     * Some user waited for an asynchronous process using watchdog.isWatching() which
     * is now properly implemented  using {@code DefaultExecuteResultHandler}.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec34_2() throws Exception {

        final CommandLine cmdLine = new CommandLine(pingScript);
        cmdLine.addArgument("10"); // sleep 10 secs

        final ExecuteWatchdog watchdog = new ExecuteWatchdog(5000);
        final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        exec.setWatchdog(watchdog);
        exec.execute(cmdLine, handler);
        handler.waitFor();
        assertTrue("Process has exited", handler.hasResult());
        assertNotNull("Process was aborted", handler.getException());
        assertTrue("Watchdog should have killed the process", watchdog.killedProcess());
        assertFalse("Watchdog is no longer watching the process", watchdog.isWatching());
    }
}
