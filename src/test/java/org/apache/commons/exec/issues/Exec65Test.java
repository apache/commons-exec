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

import org.apache.commons.exec.AbstractExecTest;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Test to show that watchdog can destroy 'sudo' and 'sleep'.
 *
 * @see <a href="https://issues.apache.org/jira/browse/EXEC-65">EXEC-65</a>
 */
public class Exec65Test extends AbstractExecTest {

    @Test
    @Timeout(value = TEST_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    public void testExec65WitSleepUsingSleepCommandDirectly() throws Exception {

        if (OS.isFamilyUnix()) {
            final ExecuteWatchdog watchdog = new ExecuteWatchdog(WATCHDOG_TIMEOUT);
            final DefaultExecutor executor = new DefaultExecutor();
            final CommandLine command = new CommandLine("sleep");
            command.addArgument("60");
            executor.setStreamHandler(new PumpStreamHandler(System.out, System.err));
            executor.setWatchdog(watchdog);

            assertThrows(ExecuteException.class, () -> executor.execute(command));
        } else {
            assertThrows(ExecuteException.class, () -> new ExecuteException(testNotSupportedForCurrentOperatingSystem(), 0));
        }
    }

    /**
     * This test currently only works for Mac OS X
     * <ul>
     * <li>Linux hangs on the process stream while the process is finished</li>
     * <li>Windows seems to have similar problems</li>
     * </ul>
     *
     * @TODO Fix tests for Windows & Linux
     */
    @Test
    @Timeout(value = TEST_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    public void testExec65WithSleepUsingShellScript() throws Exception {

        if (OS.isFamilyMac()) {
            final DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(System.out, System.err));
            executor.setWatchdog(new ExecuteWatchdog(WATCHDOG_TIMEOUT));
            final CommandLine command = new CommandLine(resolveTestScript("sleep"));

            assertThrows(ExecuteException.class, () -> executor.execute(command));
        } else {
            assertThrows(ExecuteException.class, () -> new ExecuteException(testNotSupportedForCurrentOperatingSystem(), 0));
        }
    }

    /**
     * This is the original code snippet from the JIRA to show that
     * killing the process actually works with JDK only but it does
     * not re-direct any streams.
     */
    @Test
    @Timeout(value = TEST_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    public void testExec65WithSleepUsingShellScriptAndJDKOnly() throws Exception {

        final Process process = Runtime.getRuntime().exec(resolveTestScript("sleep").getAbsolutePath());
        Thread.sleep(WATCHDOG_TIMEOUT);

        process.destroy();
        
        process.waitFor();

        assertTrue(process.exitValue() != 0);
    }

    /**
     * Please note that this tests make assumptions about the environment. It assumes
     * that user "root" exists and that the current user is not a "sudoer" already
     * (thereby requiring a password).
     */
    @Test
    @Timeout(value = TEST_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    public void testExec65WithSudoUsingShellScript() throws Exception {
        assumeFalse(new File(".").getAbsolutePath().contains("travis"), "Test is skipped on travis, because we have to be a sudoer "
                + "to make the other tests pass.");
        if (OS.isFamilyUnix()) {
            final DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(System.out, System.err, System.in));
            executor.setWatchdog(new ExecuteWatchdog(WATCHDOG_TIMEOUT));
            final CommandLine command = new CommandLine(resolveTestScript("issues", "exec-65"));

            assertThrows(ExecuteException.class, () -> executor.execute(command));
        } else {
            assertThrows(ExecuteException.class, () -> new ExecuteException(testNotSupportedForCurrentOperatingSystem(), 0));
        }
    }
}
