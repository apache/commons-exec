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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class DefaultExecutorTest extends TestCase {

    private Executor exec = new DefaultExecutor();
    private File testDir = new File("src/test/scripts");
    private File foreverOutputFile = new File("./target/forever.txt");
    private ByteArrayOutputStream baos;

    private File testScript = TestUtil.resolveScriptForOS(testDir + "/test");
    private File errorTestScript = TestUtil.resolveScriptForOS(testDir + "/error");
    private File foreverTestScript = TestUtil.resolveScriptForOS(testDir + "/forever");
    private File nonExistingTestScript = TestUtil.resolveScriptForOS(testDir + "/grmpffffff");
    private File redirectScript = TestUtil.resolveScriptForOS(testDir + "/redirect");
    private File pingScript = TestUtil.resolveScriptForOS(testDir + "/ping");
    private File printArgsScript = TestUtil.resolveScriptForOS(testDir + "/printargs");
    private File acroRd32Script = TestUtil.resolveScriptForOS(testDir + "/acrord32");
    private File stdinSript = TestUtil.resolveScriptForOS(testDir + "/stdin");
    private File environmentSript = TestUtil.resolveScriptForOS(testDir + "/environment");

    // Get suitable exit codes for the OS
    private static final int SUCCESS_STATUS; // test script successful exit code
    private static final int ERROR_STATUS;   // test script error exit code

    static{

        int statuses[] = TestUtil.getTestScriptCodesForOS();
        SUCCESS_STATUS=statuses[0];
        ERROR_STATUS=statuses[1];

        // turn on debug mode and throw an exception for each encountered problem
        System.setProperty("org.apache.commons.exec.lenient", "false");
        System.setProperty("org.apache.commons.exec.debug", "true");                
    }
    
    protected void setUp() throws Exception {

        System.out.println(">>> Executing " + getName() + " ...");

        // delete the marker file
        this.foreverOutputFile.getParentFile().mkdirs();
        if(this.foreverOutputFile.exists()) {
            this.foreverOutputFile.delete();
        }

        // prepare a ready to Executor
        this.baos = new ByteArrayOutputStream();
        this.exec.setStreamHandler(new PumpStreamHandler(baos, baos));
    }

    protected void tearDown() throws Exception {
        this.baos.close();
    }

    /**
     * The simplest possible test - start a script and
     * check that the output was pumped into our
     * <code>ByteArrayOutputStream</code>.
     *
     * @throws Exception the test failed
     */
    public void testExecute() throws Exception {
        CommandLine cl = new CommandLine(testScript);
        int exitValue = exec.execute(cl);
        assertEquals("FOO..", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
        assertEquals(new File("."), exec.getWorkingDirectory());        
    }

    public void testExecuteWithWorkingDirectory() throws Exception {
        File workingDir = new File("./target");
        CommandLine cl = new CommandLine(testScript);
        exec.setWorkingDirectory(workingDir);
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
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();        
        exec.execute(cl, resultHandler);
        resultHandler.waitFor();
        assertFalse(exec.isFailure(resultHandler.getExitValue()));
        assertEquals("FOO..", baos.toString().trim());
    }

    public void testExecuteAsyncWithError() throws Exception {
        CommandLine cl = new CommandLine(errorTestScript);
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        exec.execute(cl, resultHandler);
        try {
            resultHandler.waitFor();
        }
        catch(ExecuteException e) {
            assertTrue(exec.isFailure(e.getExitValue()));
            assertNotNull(resultHandler.getException());
            assertEquals("FOO..", baos.toString().trim());
            return;
        }
        fail("Expecting an ExecuteException");
    }

    /**
     * Start a asynchronous process and terminate it manually before the
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
        // terminate it using the watchdog
        watchdog.destroyProcess();
        assertTrue("Watchdog should have killed the process", watchdog.killedProcess());
        assertFalse(watchdog.isWatching());
    }

    /**
     * Start a async process and try to terminate it manually but
     * the process was already terminated by the watchdog. This is
     * basically a race condition between infrastructure and user
     * code.
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
     * kicks in killing the run away process. To make killing a process
     * more testable the "forever" scripts write each second a '.'
     * into "./target/forever.txt" (a marker file). After a test run
     * we should have a few dots in there.
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
            Thread.sleep(timeout);
            int nrOfInvocations = getOccurrences(readFile(this.foreverOutputFile), '.');
            assertTrue("killing the subprocess did not work : " + nrOfInvocations, nrOfInvocations > 5 && nrOfInvocations <= 11);
            assertTrue( executor.getWatchdog().killedProcess() );
            return;
        }
        catch(Throwable t) {
            fail(t.getMessage());    
        }

        assertTrue("Killed process should be true", executor.getWatchdog().killedProcess() );
        fail("Process did not create ExecuteException when killed");
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

      // wait for script to start
      Thread.sleep(2000);

      // our process destroyer should be initialized now
      assertNotNull("Process destroyer should exist", exec.getProcessDestroyer());
      assertEquals("Process destroyer size should be 1", 1, processDestroyer.size());
      assertTrue("Process destroyer should exist as shutdown hook", processDestroyer.isAddedAsShutdownHook());

      // terminate it and the process destroyer is detached
      watchdog.destroyProcess();
      assertTrue(watchdog.killedProcess());

      try {
          handler.waitFor();
          fail("Expecting an ExecutionException");
      }
      catch(ExecuteException e) {
          // nothing to do
      }

      assertEquals("Processor Destroyer size should be 0", 0, processDestroyer.size());
      assertFalse("Process destroyer should not exist as shutdown hook", processDestroyer.isAddedAsShutdownHook());
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
            PumpStreamHandler pumpStreamHandler = new PumpStreamHandler( baos, baos, fis );
            DefaultExecutor executor = new DefaultExecutor();
            executor.setWorkingDirectory(new File("."));
            executor.setStreamHandler( pumpStreamHandler );
            int exitValue = executor.execute(cl);
            fis.close();
            String result = baos.toString().trim();
            System.out.println(result);
            assertTrue(result.endsWith("Finished reading from stdin"));
            assertFalse(exec.isFailure(exitValue));
        }
        else if(OS.isFamilyWindows()) {
            System.err.println("The code samples to do that in windows look like a joke ... :-( .., no way I'm doing that");
            System.err.println("The test 'testExecuteWithRedirectedStreams' does not support the following OS : " + System.getProperty("os.name"));
            return;
        }
        else {
            System.err.println("The test 'testExecuteWithRedirectedStreams' does not support the following OS : " + System.getProperty("os.name"));
            return;
        }
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
     * The test script reads an argument from <code>stdin<code> and prints
     * the result to stdout. To make things slightly more interesting
     * we are using an asynchronous process.
     *
     * @throws Exception the test failed
     */
    public void testStdInHandling() throws Exception {

        ByteArrayInputStream bais = new ByteArrayInputStream("Foo\n".getBytes());
        CommandLine cl = new CommandLine(this.stdinSript);
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler( this.baos, System.err, bais);
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(pumpStreamHandler);
        executor.execute(cl, resultHandler);

        resultHandler.waitFor();

        assertTrue(resultHandler.getExitValue() == 0);
        assertTrue(this.baos.toString().indexOf("Hello Foo!") > 0);
    }

    /**
     * Call a script to dump the environment variables of the subprocess. 
     *
     * @throws Exception the test failed
     */
    public void testEnvironmentVariables() throws Exception {
        exec.execute(new CommandLine(environmentSript));
        String environment = baos.toString().trim();
        assertTrue("Found no environment variables", environment.length() > 0);
        System.out.println(environment);
    }

    // ======================================================================
    // === Testing bug fixes
    // ======================================================================

    /**
     * Test the patch for EXEC-33 (https://issues.apache.org/jira/browse/EXEC-33)
     *
     * PumpStreamHandler hangs if System.in is redirect to process input stream .
     *
     * @throws Exception the test failed
     */
    public void testExec33() throws Exception
    {
        CommandLine cl = new CommandLine(testScript);
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler( System.out, System.err, System.in );
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler( pumpStreamHandler );
        int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
    }

    /**
     * EXEC-34 https://issues.apache.org/jira/browse/EXEC-34
     *
     * Race condition prevent watchdog working using ExecuteStreamHandler.
     * The test fails because when watchdog.destroyProcess() is invoked the
     * external process is not bound to the watchdog yet
     *
     * @throws Exception the test failed
     */
    public void testExec34() throws Exception {

        CommandLine cmdLine = new CommandLine(pingScript);
        cmdLine.addArgument("10"); // sleep 10 secs

        ExecuteWatchdog watchdog = new ExecuteWatchdog(Integer.MAX_VALUE);
        DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        exec.setWatchdog(watchdog);
        exec.execute(cmdLine, handler);
        // if you comment out the next line the test will fail
        Thread.sleep(2000);
        // terminate it
        watchdog.destroyProcess();
        assertTrue("Watchdog should have killed the process",watchdog.killedProcess());
    }

    /**
     * Test EXEC-36 see https://issues.apache.org/jira/browse/EXEC-36
     *
     * Original example from Kai Hu which only can be tested on Unix
     *
     * @throws Exception the test failed
     */
    public void testExec36_1() throws Exception {

        if(OS.isFamilyUnix()) {

            CommandLine cmdl;

            /**
             * ./script/jrake cruise:publish_installers INSTALLER_VERSION=unstable_2_1 \
             *     INSTALLER_PATH="/var/lib/ cruise-agent/installers" INSTALLER_DOWNLOAD_SERVER='something' WITHOUT_HELP_DOC=true
             */

            String expected = "./script/jrake\n" +
                    "cruise:publish_installers\n" +
                    "INSTALLER_VERSION=unstable_2_1\n" +
                    "INSTALLER_PATH=\"/var/lib/ cruise-agent/installers\"\n" +
                    "INSTALLER_DOWNLOAD_SERVER='something'\n" +
                    "WITHOUT_HELP_DOC=true";

            cmdl = new CommandLine(printArgsScript);
            cmdl.addArgument("./script/jrake", false);
            cmdl.addArgument("cruise:publish_installers", false);
            cmdl.addArgument("INSTALLER_VERSION=unstable_2_1", false);
            cmdl.addArgument("INSTALLER_PATH=\"/var/lib/ cruise-agent/installers\"", false);
            cmdl.addArgument("INSTALLER_DOWNLOAD_SERVER='something'", false);
            cmdl.addArgument("WITHOUT_HELP_DOC=true", false);

            int exitValue = exec.execute(cmdl);
            String result = baos.toString().trim();
            System.out.println("=== Expected ===");
            System.out.println(expected);
            System.out.println("=== Result ===");
            System.out.println(result);
            assertFalse(exec.isFailure(exitValue));
            assertEquals(expected, result);
        }
        else {
            System.err.println("The test 'testExec36_1' does not support the following OS : " + System.getProperty("os.name"));
            return;
        }
    }

    /**
     * Test EXEC-36 see https://issues.apache.org/jira/browse/EXEC-36
     *
     * Test a complex real example found at
     * http://blogs.msdn.com/b/astebner/archive/2005/12/13/503471.aspx
     *
     * The command line is so weird that it even falls apart under Windows
     *
     * @throws Exception the test failed
     */
    public void testExec36_2() throws Exception {

        String expected;

        // the original command line
        // dotnetfx.exe /q:a /c:"install.exe /l ""\Documents and Settings\myusername\Local Settings\Temp\netfx.log"" /q"

        if(OS.isFamilyWindows()) {
            expected = "dotnetfx.exe\n" +
                "/q:a\n" +
                "/c:\"install.exe /l \"\"\\Documents and Settings\\myusername\\Local Settings\\Temp\\netfx.log\"\" /q\"";
        }
        else if(OS.isFamilyUnix()) {
            expected = "dotnetfx.exe\n" +
                "/q:a\n" +
                "/c:\"install.exe /l \"\"/Documents and Settings/myusername/Local Settings/Temp/netfx.log\"\" /q\"";
        }
        else {
            System.err.println("The test 'testExec36_3' does not support the following OS : " + System.getProperty("os.name"));
            return;
        }
        
        CommandLine cmdl;
        File file = new File("/Documents and Settings/myusername/Local Settings/Temp/netfx.log");
        Map map = new HashMap();
        map.put("FILE", file);

        cmdl = new CommandLine(printArgsScript);
        cmdl.setSubstitutionMap(map);
        cmdl.addArgument("dotnetfx.exe", false);
        cmdl.addArgument("/q:a", false);
        cmdl.addArgument("/c:\"install.exe /l \"\"${FILE}\"\" /q\"", false);

        int exitValue = exec.execute(cmdl);
        String result = baos.toString().trim();
        System.out.println("=== Expected ===");
        System.out.println(expected);
        System.out.println("=== Result ===");
        System.out.println(result);
        assertFalse(exec.isFailure(exitValue));

        if(OS.isFamilyUnix()) {
        	// the parameters fall literally apart under Windows - need to disable the check for Win32
        	assertEquals(expected, result);
        }
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
    public void testExec41WithStreams() throws Exception {

    	CommandLine cmdLine;

    	if(OS.isFamilyWindows()) {
    		cmdLine = CommandLine.parse("ping.exe -n 10 -w 1000 127.0.0.1");
    	}
    	else if(OS.isFamilyUnix()) {
    		cmdLine = CommandLine.parse("ping -c 10 127.0.0.1");
    	}
    	else {
    		System.err.println("The test 'testExec41WithStreams' does not support the following OS : " + System.getProperty("os.name"));
    		return;
    	}

		DefaultExecutor executor = new DefaultExecutor();
		ExecuteWatchdog watchdog = new ExecuteWatchdog(2*1000); // allow process no more than 2 secs
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler( System.out, System.err);
        pumpStreamHandler.setAlwaysWaitForStreamThreads(false);

		executor.setWatchdog(watchdog);
        executor.setStreamHandler(pumpStreamHandler);

		long startTime = System.currentTimeMillis();

		try {
			executor.execute(cmdLine);
		} catch (ExecuteException e) {
			// nothing to do
		}

        long duration = System.currentTimeMillis() - startTime;

		System.out.println("Process completed in " + duration +" millis; below is its output");

		if (watchdog.killedProcess()) {
			System.out.println("Process timed out and was killed by watchdog.");
		}

        assertTrue("The process was killed by the watchdog", watchdog.killedProcess());
        assertTrue("Skipping the Thread.join() did not work", duration < 9000);
    }

    /**
     * Test EXEC-41 with a disabled PumpStreamHandler to check if we could return
     * immediately after killing the process (no streams implies no blocking
     * stream pumper threads). But you have to be 100% sure that the subprocess
     * is not writing to 'stdout' and 'stderr'.
     *
     * For this test we are using the batch file - under Windows the 'ping'
     * process can't be killed (not supported by Win32) and will happily
     * run the given time (e.g. 10 seconds) even hwen the batch file is already
     * killed. 
     *
     * @throws Exception the test failed
     */
    public void testExec41WithoutStreams() throws Exception {

		CommandLine cmdLine = new CommandLine(pingScript);
		cmdLine.addArgument("10"); // sleep 10 secs
		DefaultExecutor executor = new DefaultExecutor();
		ExecuteWatchdog watchdog = new ExecuteWatchdog(2*1000); // allow process no more than 2 secs

        // create a custom "PumpStreamHandler" doing no pumping at all
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(null, null, null);
        
		executor.setWatchdog(watchdog);
        executor.setStreamHandler(pumpStreamHandler);

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

        assertTrue("The process was killed by the watchdog", watchdog.killedProcess());
        assertTrue("SKipping the Thread.join() did not work", duration < 9000);
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

    // ======================================================================
    // === Long running tests
    // ======================================================================

    /**
     * Start any processes in a loop to make sure that we do
     * not leave any handles/resources open.
     *
     * @throws Exception the test failed
     */
    public void testExecuteStability() throws Exception {

        // make a plain-vanilla test
        for(int i=0; i<1000; i++) {
            Map env = new HashMap();
            env.put("TEST_ENV_VAR", new Integer(i));
            CommandLine cl = new CommandLine(testScript);
            int exitValue = exec.execute(cl,env);
            assertFalse(exec.isFailure(exitValue));
            assertEquals("FOO." + i + ".", baos.toString().trim());
            baos.reset();
        }

        // now be nasty and use the watchdog to kill out sub-processes
        for(int i=0; i<100; i++) {
            Map env = new HashMap();
            env.put("TEST_ENV_VAR", new Integer(i));
            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
            CommandLine cl = new CommandLine(foreverTestScript);
            ExecuteWatchdog watchdog = new ExecuteWatchdog(500);
            exec.setWatchdog(watchdog);
            exec.execute(cl, env, resultHandler);
            try {
                resultHandler.waitFor();
            }
            catch(ExecuteException e) {
                // nothing to do
            }
            baos.reset();
        }
    }


    // ======================================================================
    // === Helper methods
    // ======================================================================

    private String readFile(File file) throws Exception {

        String text;
        StringBuffer contents = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(file));        

        while ((text = reader.readLine()) != null)
        {
            contents.append(text)
                .append(System.getProperty(
                    "line.separator"));
        }
        reader.close();
        return contents.toString();
    }

    private int getOccurrences(String data, char c) {

        int result = 0;

        for(int i=0; i<data.length(); i++) {
            if(data.charAt(i) == c) {
                result++;
            }
        }

        return result;
    }
}
