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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Placeholder for mailing list question - provided a minimal test case
 * to answer the question as sel-contained regression test.
 *
 * @version $Id$
 */
public class StandAloneTest {

    @BeforeClass
    public static void classSetUp() {
        System.setProperty("org.apache.commons.exec.lenient", "false");
        System.setProperty("org.apache.commons.exec.debug", "true");
    }

    @Test
    public void testMe() throws Exception {
        if (OS.isFamilyUnix()) {
            final File testScript = TestUtil.resolveScriptForOS("./src/test/scripts/standalone");
            final Executor exec = new DefaultExecutor();
            exec.setStreamHandler(new PumpStreamHandler());
            final CommandLine cl = new CommandLine(testScript);
            exec.execute(cl);
            assertTrue(new File("./target/mybackup.gz").exists());
        }
    }
}
