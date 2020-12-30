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
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Test EXEC-57 (https://issues.apache.org/jira/browse/EXEC-57).
 *
 */
public class Exec57Test extends AbstractExecTest {

    /**
     * DefaultExecutor.execute() does not return even if child process terminated - in this
     * case the child process hangs because the grand children is connected to stdout & stderr
     * and is still running. As work-around a stop timeout is used for the PumpStreamHandler
     * to ensure that the caller does not block forever but if the stop timeout is exceeded
     * an ExecuteException is thrown to notify the caller. But this case the threads are still
     * around causing a resource leak.
     *
     * @TODO [EXEC-57] Broken for Mac OS X & Linux
     */
    @Disabled("Broken for Unix-based systems")
    @Test
    @Timeout(value = TEST_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    public void testExecutionOfBackgroundProcess() throws IOException {

        final CommandLine cmdLine = new CommandLine("sh").addArgument("-c").addArgument("./src/test/scripts/issues/exec-57-nohup.sh", false);
        final DefaultExecutor executor = new DefaultExecutor();
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(System.out, System.err);

        executor.setStreamHandler(pumpStreamHandler);

        executor.execute(cmdLine);
    }

    /**
     * The same approach using a completely detached process works nicely on Mac OS X.
     *
     * @throws IOException
     */
    @Test
    @Timeout(value = TEST_TIMEOUT, unit = TimeUnit.MILLISECONDS)
    public void testExecutionOfDetachedProcess() throws IOException {

        if (!OS.isFamilyUnix()) {
            testNotSupportedForCurrentOperatingSystem();
            return;
        }

        final CommandLine cmdLine = new CommandLine("sh").addArgument("-c").addArgument("./src/test/scripts/issues/exec-57-detached.sh", false);
        final DefaultExecutor executor = new DefaultExecutor();
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(System.out, System.err);

        executor.setStreamHandler(pumpStreamHandler);

        executor.execute(cmdLine);
    }
}
