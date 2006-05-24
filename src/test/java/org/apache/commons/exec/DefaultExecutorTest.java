/* 
 * Copyright 2005  The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

    private String testDir = "src/test/scripts";
    private ByteArrayOutputStream baos;
    private String testScript = TestUtil.resolveScriptForOS(testDir + "/test");

    protected void setUp() throws Exception {
        baos = new ByteArrayOutputStream();
    }

    public void testExecute() throws Exception {
        Executor exec = new DefaultExecutor();
        exec.setStreamHandler(new PumpStreamHandler(baos, baos));

        CommandLine cl = new CommandLine(new File(testScript).getAbsolutePath());

        int exitValue = exec.execute(cl);
        assertEquals("FOO..", baos.toString().trim());
        assertEquals(0, exitValue);
    }

    public void testExecuteWithArg() throws Exception {
        Executor exec = new DefaultExecutor();
        exec.setStreamHandler(new PumpStreamHandler(baos, baos));

        CommandLine cl = new CommandLine(testScript);
        cl.addArgument("BAR");
        int exitValue = exec.execute(cl);

        assertEquals("FOO..BAR", baos.toString().trim());
        assertEquals(0, exitValue);
    }

    public void testExecuteWithEnv() throws Exception {
        Executor exec = new DefaultExecutor();
        exec.setStreamHandler(new PumpStreamHandler(baos, baos));
    	
    	Map env = new HashMap();
        env.put("TEST_ENV_VAR", "XYZ");

        CommandLine cl = new CommandLine(testScript);

        int exitValue = exec.execute(cl, env);

        assertEquals("FOO.XYZ.", baos.toString().trim());
        assertEquals(0, exitValue);
    }

    public void disabledtestExecuteAsync() throws Exception {
        Executor exec = new DefaultExecutor();
        exec.setStreamHandler(new PumpStreamHandler(baos, baos));
        
        CommandLine cl = new CommandLine(testScript);
        
        MockExecuteResultHandler handler = new MockExecuteResultHandler();
        
        exec.execute(cl, handler);
        
        // wait for script to run
        Thread.sleep(1000);
        
        assertEquals("FOO..", baos.toString().trim());
        assertEquals(0, handler.getExitValue());
    }
    
    

    protected void tearDown() throws Exception {
        baos.close();
    }
}
