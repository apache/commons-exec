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

import org.apache.commons.exec.*;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * Test to show that watchdog can destroy 'sudo' and 'sleep'.
 *
 * @see <a href="https://issues.apache.org/jira/browse/EXEC-65">EXEC-65</a>
 */
public class Exec65Test {

    private static final int TIMEOUT = 3000;
    private final File testDir = new File("src/test/scripts");

    @Test(expected = ExecuteException.class, timeout = 15000)
    public void testExec65WitSleepUsingCommandLine() throws Exception
    {
        if(OS.isFamilyUnix())
        {
            final DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(System.out, System.err));
            final ExecuteWatchdog watchdog = new ExecuteWatchdog(TIMEOUT);
            executor.setWatchdog(watchdog);
            final CommandLine command = new CommandLine("sleep");
            command.addArgument("60");

            executor.execute(command);
        }
    }

    @Test(expected = ExecuteException.class, timeout = 15000)
    public void testExec65WithSleepUsingShellScript() throws Exception
    {
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(System.out, System.err));
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(TIMEOUT);
        executor.setWatchdog(watchdog);
        final CommandLine command = new CommandLine(TestUtil.resolveScriptForOS(testDir + "/sleep"));

        executor.execute(command);
    }

    @Test(timeout = 15000)
    public void testExec65WithSleepUsingShellScriptAndRuntimeDirectly() throws Exception
    {
        Process process = Runtime.getRuntime().exec(TestUtil.resolveScriptForOS(testDir + "/sleep").getAbsolutePath());
        Thread.sleep(3000);

        process.destroy();

        while(process.isAlive()) {
            Thread.sleep(50);
        }

        assertTrue(process.exitValue() != 0);
    }

    /**
     * Please note that this tests make assumptions about the environment. It assumes
     * that user "root" exists and that the current user is not a "sudoer" already.
     */
    @Test(expected = ExecuteException.class, timeout = 15000)
    public void testExec65WithSudoUsingShellScript() throws Exception
    {
        if(OS.isFamilyUnix())
        {
            final DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(System.out, System.err, System.in));
            final ExecuteWatchdog watchdog = new ExecuteWatchdog(TIMEOUT);
            executor.setWatchdog(watchdog);
            final CommandLine command = new CommandLine(TestUtil.resolveScriptForOS(testDir + "/issues/exec-65"));

            executor.execute(command);
        }
    }
}
