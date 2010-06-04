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
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class DefaultExecutorTest extends TestCase {

    private Executor exec = new DefaultExecutor();
    private File testDir = new File("src/test/scripts");
    private ByteArrayOutputStream baos;
    private File testScript = TestUtil.resolveScriptForOS(testDir + "/test");
    private File errorTestScript = TestUtil.resolveScriptForOS(testDir + "/error");
    private File foreverTestScript = TestUtil.resolveScriptForOS(testDir + "/forever");
    private File nonExistingTestScript = TestUtil.resolveScriptForOS(testDir + "/grmpffffff");
    private File redirectScript = TestUtil.resolveScriptForOS(testDir + "/redirect");
    private File exec41Script = TestUtil.resolveScriptForOS(testDir + "/exec41");
    private File printArgsScript = TestUtil.resolveScriptForOS(testDir + "/printargs");
    private File acroRd32Script = TestUtil.resolveScriptForOS(testDir + "/acrord32");


    // Get suitable exit codes for the OS
    private static final int SUCCESS_STATUS; // test script successful exit code
    private static final int ERROR_STATUS;   // test script error exit code
    static{
       int statuses[] = TestUtil.getTestScriptCodesForOS();
       SUCCESS_STATUS=statuses[0];
       ERROR_STATUS=statuses[1];
    }
    
    protected void setUp() throws Exception {
        baos = new ByteArrayOutputStream();
        exec.setStreamHandler(new PumpStreamHandler(baos, baos));
    }

    protected void tearDown() throws Exception {
        baos.close();
    }

    public void testExecute() throws Exception {
        CommandLine cl = new CommandLine(testScript);

        int exitValue = exec.execute(cl);
        assertEquals("FOO..", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
    }

    public void testExecuteWithWorkingDirectory() throws Exception {
        File workingDir = new File(".");
        CommandLine cl = new CommandLine(testScript);
        exec.setWorkingDirectory(new File("."));
        int exitValue = exec.execute(cl);
        assertEquals("FOO..", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
        assertEquals(exec.getWorkingDirectory(), workingDir);
    }

    public void testExecuteWithInvalidWorkingDirectory() throws Exception {
        File workingDir = new File("/foo/bar");
        CommandLine cl = new CommandLine(testScript);
        exec.setWorkingDirectory(workingDir);
        try {
            exec.execute(cl);
            fail("Expected exception due to invalid working directory");
        }
        catch(IOException e) {
            return;
        }
    }

    public void testExecuteWithError() throws Exception {
        CommandLine cl = new CommandLine(errorTestScript);
        
        try{
            exec.execute(cl);
            fail("Must throw ExecuteException");
        } catch(ExecuteException e) {
            assertTrue(exec.isFailure(e.getExitValue()));
        }
    }

    public void testExecuteWithArg() throws Exception {
        CommandLine cl = new CommandLine(testScript);
        cl.addArgument("BAR");
        int exitValue = exec.execute(cl);

        assertEquals("FOO..BAR", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
    }

    public void testExecuteWithEnv() throws Exception {
    	Map env = new HashMap();
        env.put("TEST_ENV_VAR", "XYZ");

        CommandLine cl = new CommandLine(testScript);

        int exitValue = exec.execute(cl, env);

        assertEquals("FOO.XYZ.", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
    }

    public void testExecuteAsync() throws Exception {
        CommandLine cl = new CommandLine(testScript);
        
        DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        
        exec.execute(cl, handler);
        
        // wait for script to run
        Thread.sleep(2000);
        
        assertFalse(exec.isFailure(handler.getExitValue()));
        assertEquals("FOO..", baos.toString().trim());
    }

    public void testExecuteAsyncWithError() throws Exception {
        CommandLine cl = new CommandLine(errorTestScript);
        
        DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        
        exec.execute(cl, handler);
        
        // wait for script to run
        Thread.sleep(2000);
        
        assertTrue(exec.isFailure(handler.getExitValue()));
        assertNotNull(handler.getException());
        assertEquals("FOO..", baos.toString().trim());
    }

    /**
     * Start a async process and terminate it manually before the
     * watchdog timeout occurs.
     *
     * @throws Exception the test failed 
     */
    public void testExecuteAsyncWithTimelyUserTermination() throws Exception {
        CommandLine cl = new CommandLine(foreverTestScript);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(Integer.MAX_VALUE);
        exec.setWatchdog(watchdog);
        DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        exec.execute(cl, handler);
        // wait for script to run
        Thread.sleep(2000);
        assertTrue("Watchdog should watch the process", watchdog.isWatching());
        // terminate it
        watchdog.destroyProcess();
        assertTrue("Watchdog should have killed the process",watchdog.killedProcess());
        assertFalse(watchdog.isWatching());
    }

    /**
     * Start a async process and try to terminate it manually but
     * the process was already terminated by the watchdog.
     *
     * @throws Exception the test failed
     */
    public void testExecuteAsyncWithTooLateUserTermination() throws Exception {
        CommandLine cl = new CommandLine(foreverTestScript);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(3000);
        exec.setWatchdog(watchdog);
        DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        exec.execute(cl, handler);
        // wait for script to be terminated by the watchdog
        Thread.sleep(6000);
        // try to terminate the already terminated process
        watchdog.destroyProcess();
        assertTrue("Watchdog should have killed the process already",watchdog.killedProcess());
        assertFalse(watchdog.isWatching());
    }

    /**
     * Start a script looping forever and check if the ExecuteWatchdog
     * kicks in killing the run away process.
     *
     * @throws Exception the test failed
     */
    public void testExecuteWatchdog() throws Exception {
        long timeout = 10000;
        CommandLine cl = new CommandLine(foreverTestScript);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File("."));
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        executor.setWatchdog(watchdog);
        try {
            executor.execute(cl);
        }
        catch(ExecuteException e) {
            assertTrue( executor.getWatchdog().killedProcess() );
            return;
        }
        catch(Throwable t) {
            fail(t.getMessage());    
        }

        assertTrue("Killed process should be true", executor.getWatchdog().killedProcess() );
        fail("Process did not create Execute Exception when killed");
    }

    /**
     * Try to start an non-existing application which should result
     * in an exception.
     *
     * @throws Exception the test failed
     */
    public void testExecuteNonExistingApplication() throws Exception {
        CommandLine cl = new CommandLine(nonExistingTestScript);
        DefaultExecutor executor = new DefaultExecutor();
        try {
            executor.execute(cl);
        }
        catch( IOException e) {
            // expected
            return;
        }
        fail("Got no exception when executing an non-existing application");                
    }

    /**
     * Try to start an non-existing application asynchronously which should result
     * in an exception.
     *
     * @throws Exception the test failed
     */
    public void testExecuteAsyncWithNonExistingApplication() throws Exception {
        CommandLine cl = new CommandLine(nonExistingTestScript);
        DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        exec.execute(cl, handler);
        Thread.sleep(2000);
        assertNotNull(handler.getException());
        assertTrue(exec.isFailure(handler.getExitValue()));
    }

    /**
     * Start any processes in a loop to make sure that we do
     * not leave any handles/resources open.
     *
     * @throws Exception the test failed
     */
    public void testExecuteStability() throws Exception {
        for(int i=0; i<1000; i++) {
            Map env = new HashMap();
            env.put("TEST_ENV_VAR", new Integer(i));
            CommandLine cl = new CommandLine(testScript);
            int exitValue = exec.execute(cl,env);
            assertFalse(exec.isFailure(exitValue));
            assertEquals("FOO." + i + ".", baos.toString().trim());
            baos.reset();
        }
    }

    /**
     * Invoke the error script but define that the ERROR_STATUS is a good
     * exit value and therefore no exception should be thrown.
     *
     * @throws Exception the test failed
     */
    public void testExecuteWithCustomExitValue1() throws Exception {
        exec.setExitValue(ERROR_STATUS);
        CommandLine cl = new CommandLine(errorTestScript);
        exec.execute(cl);
    }

    /**
     * Invoke the error script but define that SUCCESS_STATUS is a bad
     * exit value and therefore an exception should be thrown.
     *
     * @throws Exception the test failed
     */
    public void testExecuteWithCustomExitValue2() throws Exception {
        CommandLine cl = new CommandLine(errorTestScript);
        exec.setExitValue(SUCCESS_STATUS);
        try{
            exec.execute(cl);
            fail("Must throw ExecuteException");
        } catch(ExecuteException e) {
            assertTrue(exec.isFailure(e.getExitValue()));
            return;
        }
    }

    /**
     * Test the proper handling of ProcessDestroyer for an synchronous process.
     *
     * @throws Exception the test failed
     */
    public void testExecuteWithProcessDestroyer() throws Exception {

      CommandLine cl = new CommandLine(testScript);
      ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();
      exec.setProcessDestroyer(processDestroyer);
      
      assertTrue(processDestroyer.size() == 0);
      assertTrue(processDestroyer.isAddedAsShutdownHook() == false);
      
      int exitValue = exec.execute(cl);

      assertEquals("FOO..", baos.toString().trim());
      assertFalse(exec.isFailure(exitValue));
      assertTrue(processDestroyer.size() == 0);
      assertTrue(processDestroyer.isAddedAsShutdownHook() == false);
    }
  
    /**
     * Test the proper handling of ProcessDestroyer for an asynchronous process.
     * Since we do not terminate the process it will be terminated in the
     * ShutdownHookProcessDestroyer implementation.
     *
     * @throws Exception the test failed
     */
    public void testExecuteAsyncWithProcessDestroyer() throws Exception {

      CommandLine cl = new CommandLine(foreverTestScript);
      DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
      ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();
      ExecuteWatchdog watchdog = new ExecuteWatchdog(Integer.MAX_VALUE);

      assertTrue(exec.getProcessDestroyer() == null);
      assertTrue(processDestroyer.size() == 0);
      assertTrue(processDestroyer.isAddedAsShutdownHook() == false);

      exec.setWatchdog(watchdog);
      exec.setProcessDestroyer(processDestroyer);
      exec.execute(cl, handler);

      // wait for script to run
      Thread.sleep(2000);
      assertNotNull("Process destroyer should exist",exec.getProcessDestroyer());      
      assertEquals("Process destroyer size should be 1",1,processDestroyer.size());
      assertTrue("Process destroyer should exist as shutdown hook",processDestroyer.isAddedAsShutdownHook());

      // terminate it and the process destroyer is detached
      watchdog.destroyProcess();
      assertTrue(watchdog.killedProcess());
      Thread.sleep(100);
      assertEquals("Processor Destroyer size should be 0",0,processDestroyer.size());
      assertFalse("Process destroyer should not exist as shutdown hook",processDestroyer.isAddedAsShutdownHook());
    }

    /**
     * Invoke the test using some fancy arguments.
     *
     * @throws Exception the test failed
     */
    public void testExecuteWithFancyArg() throws Exception {
        CommandLine cl = new CommandLine(testScript);
        cl.addArgument("test $;`(0)[1]{2}");
        int exitValue = exec.execute(cl);
        assertTrue(baos.toString().trim().indexOf("test $;`(0)[1]{2}") > 0);
        assertFalse(exec.isFailure(exitValue));
    }

    /**
     * Start a process with redirected streams - stdin of the newly
     * created process is connected to a FileInputStream whereas
     * the "redirect" script reads all lines from stdin and prints
     * them on stdout. Furthermore the script prints a status
     * message on stderr.
     *
     * @throws Exception the test failed
     */
    public void testExecuteWithRedirectedStreams() throws Exception
    {
        if(OS.isFamilyUnix())
        {
            FileInputStream fis = new FileInputStream("./NOTICE.txt");
            CommandLine cl = new CommandLine(redirectScript);
            PumpStreamHandler pumpStreamHandler = new PumpStreamHandler( System.out, System.out, fis );
            DefaultExecutor executor = new DefaultExecutor();
            executor.setWorkingDirectory(new File("."));
            executor.setStreamHandler( pumpStreamHandler );
            int exitValue = executor.execute(cl);
            fis.close();
            assertFalse(exec.isFailure(exitValue));
        }
    }

    /**
     * Start a process and connect stdin, stdout and stderr (see EXEC-33).
     *
     * @throws Exception the test failed
     */
    public void testExecuteWithStdin() throws Exception
    {
        CommandLine cl = new CommandLine(testScript);
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler( System.out, System.err, System.in );
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler( pumpStreamHandler );
        int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
    }

     /**
      * Start a process and connect stdout and stderr.
      *
      * @throws Exception the test failed
      */
     public void testExecuteWithStdOutErr() throws Exception
     {
         CommandLine cl = new CommandLine(testScript);
         PumpStreamHandler pumpStreamHandler = new PumpStreamHandler( System.out, System.err );
         DefaultExecutor executor = new DefaultExecutor();
         executor.setStreamHandler( pumpStreamHandler );
         int exitValue = executor.execute(cl);
         assertFalse(exec.isFailure(exitValue));
     }

     /**
      * Start a process and connect it to no stream.
      *
      * @throws Exception the test failed
      */
     public void testExecuteWithNullOutErr() throws Exception
     {
         CommandLine cl = new CommandLine(testScript);
         PumpStreamHandler pumpStreamHandler = new PumpStreamHandler( null, null );
         DefaultExecutor executor = new DefaultExecutor();
         executor.setStreamHandler( pumpStreamHandler );
         int exitValue = executor.execute(cl);
         assertFalse(exec.isFailure(exitValue));
     }

     /**
      * Start a process and connect out and err to a file.
      *
      * @throws Exception the test failed
      */
     public void testExecuteWithRedirectOutErr() throws Exception
     {
         File outfile = File.createTempFile("EXEC", ".test");
         outfile.deleteOnExit();
         CommandLine cl = new CommandLine(testScript);
         PumpStreamHandler pumpStreamHandler = new PumpStreamHandler( new FileOutputStream(outfile) );
         DefaultExecutor executor = new DefaultExecutor();
         executor.setStreamHandler( pumpStreamHandler );
         int exitValue = executor.execute(cl);
         assertFalse(exec.isFailure(exitValue));
         assertTrue(outfile.exists());
     }

    /**
     * Test the patch for EXEC-41 (https://issues.apache.org/jira/browse/EXEC-41).
     *
     * When a process runs longer than allowed by a configured watchdog's
     * timeout, the watchdog tries to destroy it and then DefaultExecutor
     * tries to clean up by joining with all installed pump stream threads.
     * Problem is, that sometimes the native process doesn't die and thus
     * streams aren't closed and the stream threads do not complete.
     *
     * The patch provides setAlwaysWaitForStreamThreads(boolean) method
     * in PumpStreamHandler. By default, alwaysWaitForStreamThreads is set
     * to true to preserve the current behavior. If set to false, and
     * process is killed by watchdog, DefaultExecutor's call into
     * ErrorStreamHandler.stop will NOT join the stream threads and
     * DefaultExecutor will NOT attempt to close the streams, so the
     * executor's thread won't get stuck.
     *
     * @throws Exception the test failed
     */
    public void testExec41() throws Exception {

		CommandLine cmdLine = new CommandLine(exec41Script);
		cmdLine.addArgument("10"); // sleep 10 secs
		DefaultExecutor executor = new DefaultExecutor();
		ExecuteWatchdog watchdog = new ExecuteWatchdog(2*1000); // allow process no more than 2 secs
		executor.setWatchdog(watchdog);

		long startTime = System.currentTimeMillis();

		try {
			executor.execute(cmdLine);
		} catch (ExecuteException e) {
			System.out.println(e);
		}

        long duration = System.currentTimeMillis() - startTime;
        
		System.out.println("Process completed in " + duration +" millis; below is its output");

		if (watchdog.killedProcess()) {
			System.out.println("Process timed out and was killed.");
		}

        assertTrue("The process was not killed by the watchdog", watchdog.killedProcess());
    }

    /**
     * A generic test case to print the command line arguments to 'printargs' script to solve
     * even more command line puzzles.
     *
     * @throws Exception the test failed
     */
    public void testExecuteWithComplexArguments() throws Exception {
        CommandLine cl = new CommandLine(printArgsScript);
        cl.addArgument("gdal_translate");
        cl.addArgument("HDF5:\"/home/kk/grass/data/4404.he5\"://HDFEOS/GRIDS/OMI_Column_Amount_O3/Data_Fields/ColumnAmountO3/home/kk/4.tif", false);
        DefaultExecutor executor = new DefaultExecutor();
        int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
     }

    /**
     * Test EXEC-44 (https://issues.apache.org/jira/browse/EXEC-44).
     *
     * Because the ExecuteWatchdog is the only way to destroy asynchronous
     * processes, it should be possible to set it to an infinite timeout,
     * for processes which should not timeout, but manually destroyed
     * under some circumstances.
     *
     * @throws Exception the test failed
     */
    public void testExec44() throws Exception {

        CommandLine cl = new CommandLine(foreverTestScript);
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);

        exec.setWatchdog(watchdog);
        exec.execute(cl, resultHandler);

        // wait for script to run
        Thread.sleep(5000);
        assertTrue("The watchdog is watching the process", watchdog.isWatching());       

        // terminate it
        watchdog.destroyProcess();
        assertTrue("The watchdog has killed the process", watchdog.killedProcess());
        assertFalse("The watchdog is no longer watching any process", watchdog.isWatching());
    }


    /**
     * Runs the final tutorial example. The sample demonstrates the following
     * features of commons-exec
     * <ul>
     *  <li>creating a command line with parameter substitution
     *  <li>use a watchdog to kill a run-away process after a certain timeout
     *  <li>use a 'resultHandler' to execute the process asynchronously
     *  <li>check if the process cleanly exited or was killed by the watchdog
     * </ul>
     *
     * @throws Exception the test failed
     */

    public void testTutorialExample() throws Exception {

        final long timeout1 = 5*1000;
        final long timeout2 = 8*1000;

        // build up the command line
        CommandLine commandLine = CommandLine.parse(this.acroRd32Script.getAbsolutePath());
        commandLine.addArgument("/p");
        commandLine.addArgument("/h");
        commandLine.addArgument("${file}");
        HashMap map = new HashMap();
        map.put("file", "./pom.xml");
        commandLine.setSubstitutionMap(map);

        // asynchronous execution is defined by using a 'resultHander'
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler() {
            public void onProcessComplete(int exitValue) {
                super.onProcessComplete(exitValue);
                System.out.println("[resultHandler] The print process has exited with the following value: " + exitValue);
            }
            public void onProcessFailed(ExecuteException e) {
                super.onProcessFailed(e);
                System.err.println("[resultHandler] The print process was not successfully executed due to : " + e.getMessage());
            }
        };

        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout1);

        // execute the asynchronous process and consider '1' as success
        Executor executor = new DefaultExecutor();
        executor.setExitValue(1);
        executor.setStreamHandler(new PumpStreamHandler());
        executor.setWatchdog(watchdog);
        executor.execute(commandLine, resultHandler);

        // wait for some time (longer than the timeout for the watchdog to avoid race conditions)
        Thread.sleep(timeout2);
 
        if(resultHandler.hasResult()) {
            if(resultHandler.getException() != null) {
                System.err.println("[application] The document was NOT successfully printed");
            }
            else {
                System.out.println("[application] The document was successfully printed");
            }
        }
        else {
            if(watchdog.killedProcess()) {
                System.err.println("[application] The print process was killed by the watchdog");
            }
            else {
                System.err.println("[application] The print process is still running");
            }
        }


    }
}
