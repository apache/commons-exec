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

import java.io.File;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.TestUtil;
import org.junit.Test;

/**
 * Test the patch for EXEC-33 (https://issues.apache.org/jira/browse/EXEC-33)
 *
 * PumpStreamHandler hangs if System.in is redirect to process input stream .
 *
 * @version $Id$
 */
public class Exec33Test {

    private final Executor exec = new DefaultExecutor();
    private final File testDir = new File("src/test/scripts");
    private final File testScript = TestUtil.resolveScriptForOS(testDir + "/test");

    @Test
    public void testExec33() throws Exception {
        final CommandLine cl = new CommandLine(testScript);
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(System.out, System.err, System.in);
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(pumpStreamHandler);
        final int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
    }
}
