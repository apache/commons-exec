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

import junit.framework.TestCase;

import java.io.File;
import java.io.OutputStream;

/**
 * Test the LogOutputStream.
 */
public class LogOutputStreamTest extends TestCase
{

    private Executor exec = new DefaultExecutor();
    private File testDir = new File("src/test/scripts");
    private OutputStream systemOut;
    private File environmentScript = TestUtil.resolveScriptForOS(testDir + "/environment");

    static{
        // turn on debug mode and throw an exception for each encountered problem
        System.setProperty("org.apache.commons.exec.lenient", "false");
        System.setProperty("org.apache.commons.exec.debug", "true");
    }


    protected void setUp() throws Exception {
        this.systemOut = new SystemLogOutputStream(1);
        this.exec.setStreamHandler(new PumpStreamHandler(systemOut, systemOut));
    }

    protected void tearDown() throws Exception {
        this.systemOut.close();
    }

    // ======================================================================
    // Start of regression tests
    // ======================================================================

    public void testStdout() throws Exception {
        CommandLine cl = new CommandLine(environmentScript);
        int exitValue = exec.execute(cl);
        assertFalse(exec.isFailure(exitValue));
    }

    // ======================================================================
    // Helper classes
    // ======================================================================

    private class SystemLogOutputStream extends LogOutputStream {

        private SystemLogOutputStream(int level) {
            super(level);
        }

        protected void processLine(String line, int level) {
            System.out.println(line);
        }
    }

}
