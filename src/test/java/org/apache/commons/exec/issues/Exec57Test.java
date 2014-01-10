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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.junit.Test;

/**
 * Test EXEC-57 (https://issues.apache.org/jira/browse/EXEC-57).
 *
 * @version $Id$
 */
public class Exec57Test {

    private final File testDir = new File("src/test/scripts");


    /**
     *
     * DefaultExecutor.execute() does not return even if child process terminated - in this
     * case the child process hangs because the grand children is connected to stdout & stderr
     * and is still running. As work-around a stop timeout is used for the PumpStreamHandler
     * to ensure that the caller does not block forever but if the stop timeout is exceeded
     * an ExecuteException is thrown to notify the caller.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec_57() throws IOException {

        if (!OS.isFamilyUnix()) {
            System.err.println("The test 'testSyncInvocationOfBackgroundProcess' does not support the following OS : " + System.getProperty("os.name"));
            return;
        }

        final CommandLine cmdLine = new CommandLine("sh").addArgument("-c").addArgument(testDir + "/invoker.sh", false);

        final DefaultExecutor executor = new DefaultExecutor();
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(System.out, System.err);

        // Without this timeout current thread will be blocked
        // even if command we'll invoke will terminate immediately.
        pumpStreamHandler.setStopTimeout(2000);
        executor.setStreamHandler(pumpStreamHandler);
        final long startTime = System.currentTimeMillis();

        System.out.println("Executing " + cmdLine);

        try {
            executor.execute(cmdLine);
        }
        catch (final ExecuteException e) {
            final long duration = System.currentTimeMillis() - startTime;
            System.out.println("Process completed in " + duration +" millis; above is its output");
            return;
        }

        fail("Expecting an ExecuteException");
    }
}
