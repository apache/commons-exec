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

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the LogOutputStream.
 *
 * @version $Id$
 */
public class LogOutputStreamTest {

    private final Executor exec = new DefaultExecutor();
    private final File testDir = new File("src/test/scripts");
    private OutputStream systemOut;
    private final File environmentScript = TestUtil.resolveScriptForOS(testDir + "/environment");

    @BeforeClass
    public static void classSetUp() {
        // turn on debug mode and throw an exception for each encountered problem
        System.setProperty("org.apache.commons.exec.lenient", "false");
        System.setProperty("org.apache.commons.exec.debug", "true");
    }

    @Before
    public void setUp() throws Exception {
        this.systemOut = new SystemLogOutputStream(1);
        this.exec.setStreamHandler(new PumpStreamHandler(systemOut, systemOut));
    }

    @After
    public void tearDown() throws Exception {
        this.systemOut.close();
    }

    // ======================================================================
    // Start of regression tests
    // ======================================================================

    @Test
    public void testStdout() throws Exception {
        final CommandLine cl = new CommandLine(environmentScript);
        final int exitValue = exec.execute(cl);
        assertFalse(exec.isFailure(exitValue));
    }

    // ======================================================================
    // Helper classes
    // ======================================================================

    private class SystemLogOutputStream extends LogOutputStream {

        private SystemLogOutputStream(final int level) {
            super(level);
        }

        @Override
        protected void processLine(final String line, final int level) {
            System.out.println(line);
        }
    }

}
