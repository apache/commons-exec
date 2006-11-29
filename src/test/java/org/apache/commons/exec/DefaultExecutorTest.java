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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class DefaultExecutorTest extends TestCase {

    
    private Executor exec = new DefaultExecutor();
    private File testDir = new File("src/test/scripts");
    private ByteArrayOutputStream baos;
    private File testScript = TestUtil.resolveScriptForOS(testDir + "/test");
    private File errorTestScript = TestUtil.resolveScriptForOS(testDir + "/error");

    protected void setUp() throws Exception {
        baos = new ByteArrayOutputStream();
        exec.setStreamHandler(new PumpStreamHandler(baos, baos));
    }

    public void testExecute() throws Exception {
        CommandLine cl = new CommandLine(testScript);

        int exitValue = exec.execute(cl);
        assertEquals("FOO..", baos.toString().trim());
        assertEquals(0, exitValue);
    }

    public void testExecuteWithError() throws Exception {
        CommandLine cl = new CommandLine(errorTestScript);
        
        try{
            exec.execute(cl);
            fail("Must throw ExecuteException");
        } catch(ExecuteException e) {
            assertEquals(1, e.getExitValue());
        }
    }

    public void testExecuteWithArg() throws Exception {
        CommandLine cl = new CommandLine(testScript);
        cl.addArgument("BAR");
        int exitValue = exec.execute(cl);

        assertEquals("FOO..BAR", baos.toString().trim());
        assertEquals(0, exitValue);
    }

    public void testExecuteWithEnv() throws Exception {
    	Map env = new HashMap();
        env.put("TEST_ENV_VAR", "XYZ");

        CommandLine cl = new CommandLine(testScript);

        int exitValue = exec.execute(cl, env);

        assertEquals("FOO.XYZ.", baos.toString().trim());
        assertEquals(0, exitValue);
    }

    public void testExecuteAsync() throws Exception {
        CommandLine cl = new CommandLine(testScript);
        
        MockExecuteResultHandler handler = new MockExecuteResultHandler();
        
        exec.execute(cl, handler);
        
        // wait for script to run
        Thread.sleep(2000);
        
        assertEquals(0, handler.getExitValue());
        assertEquals("FOO..", baos.toString().trim());
    }

    public void testExecuteAsyncWithError() throws Exception {
        CommandLine cl = new CommandLine(errorTestScript);
        
        MockExecuteResultHandler handler = new MockExecuteResultHandler();
        
        exec.execute(cl, handler);
        
        // wait for script to run
        Thread.sleep(2000);
        
        assertEquals(1, handler.getExitValue());
        assertTrue(handler.getException() instanceof ExecuteException);
        assertEquals("FOO..", baos.toString().trim());
    }
    
    

    protected void tearDown() throws Exception {
        baos.close();
    }
}
