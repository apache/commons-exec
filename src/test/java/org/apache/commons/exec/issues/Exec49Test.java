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

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.junit.Test;

/**
 * Test EXEC-44 (https://issues.apache.org/jira/browse/EXEC-44).
 *
 * @version $Id$
 */
public class Exec49Test {

    private final Executor exec = new DefaultExecutor();

    /**
     * The issue was detected when trying to capture stdout/stderr with a PipedOutputStream and
     * then pass that to a PipedInputStream. The following code will produce the error.
     * The reason for the error is the PipedOutputStream is not being closed correctly,
     * causing the PipedInputStream to break.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec49_1() throws Exception {

        if (OS.isFamilyUnix()) {

            final CommandLine cl = CommandLine.parse("/bin/ls");
            cl.addArgument("/opt");

            // redirect stdout/stderr to pipedOutputStream
            final PipedOutputStream pipedOutputStream = new PipedOutputStream();
            final PumpStreamHandler psh = new PumpStreamHandler(pipedOutputStream);
            exec.setStreamHandler(psh);

            // start an asynchronous process to enable the main thread
            System.out.println("Preparing to execute process - commandLine=" + cl.toString());
            final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
            exec.execute(cl, handler);
            System.out.println("Process spun off successfully - process=" + cl.getExecutable());

            int x;
            final PipedInputStream pis = new PipedInputStream(pipedOutputStream);
            while ((x = pis.read()) >= 0) {
//                 System.out.println("pis.available() " + pis.available());
//                 System.out.println("x " + x);
            }
            pis.close();

            handler.waitFor(10000);
            handler.getExitValue(); // will fail if process has not finished
        }
    }

    /**
     * The issue was detected when trying to capture stdout with a PipedOutputStream and
     * then pass that to a PipedInputStream. The following code will produce the error.
     * The reason for the error is the PipedOutputStream is not being closed correctly,
     * causing the PipedInputStream to break.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec49_2() throws Exception {

        if (OS.isFamilyUnix()) {

            final CommandLine cl = CommandLine.parse("/bin/ls");
            cl.addArgument("/opt");

            // redirect only stdout to pipedOutputStream
            final PipedOutputStream pipedOutputStream = new PipedOutputStream();
            final PumpStreamHandler psh = new PumpStreamHandler(pipedOutputStream, new ByteArrayOutputStream());
            exec.setStreamHandler(psh);

            // start an asynchronous process to enable the main thread
            System.out.println("Preparing to execute process - commandLine=" + cl.toString());
            final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
            exec.execute(cl, handler);
            System.out.println("Process spun off successfully - process=" + cl.getExecutable());

            int x;
            final PipedInputStream pis = new PipedInputStream(pipedOutputStream);
            while ((x = pis.read()) >= 0) {
//                 System.out.println("pis.available() " + pis.available());
//                 System.out.println("x " + x);
            }
            pis.close();

            handler.waitFor(10000);
            handler.getExitValue(); // will fail if process has not finished
        }
    }

}
