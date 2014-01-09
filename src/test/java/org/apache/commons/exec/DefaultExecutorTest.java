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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.environment.EnvironmentUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @version $Id$
 */
public class DefaultExecutorTest {

    /** Maximum time to wait (15s) */
    private static final int WAITFOR_TIMEOUT = 15000;

    private final Executor exec = new DefaultExecutor();
    private final File testDir = new File("src/test/scripts");
    private final File foreverOutputFile = new File("./target/forever.txt");
    private ByteArrayOutputStream baos;

    private final File testScript = TestUtil.resolveScriptForOS(testDir + "/test");
    private final File errorTestScript = TestUtil.resolveScriptForOS(testDir + "/error");
    private final File foreverTestScript = TestUtil.resolveScriptForOS(testDir + "/forever");
    private final File nonExistingTestScript = TestUtil.resolveScriptForOS(testDir + "/grmpffffff");
    private final File redirectScript = TestUtil.resolveScriptForOS(testDir + "/redirect");
    private final File pingScript = TestUtil.resolveScriptForOS(testDir + "/ping");
    private final File printArgsScript = TestUtil.resolveScriptForOS(testDir + "/printargs");
//    private final File acroRd32Script = TestUtil.resolveScriptForOS(testDir + "/acrord32");
    private final File stdinSript = TestUtil.resolveScriptForOS(testDir + "/stdin");
    private final File environmentSript = TestUtil.resolveScriptForOS(testDir + "/environment");
//    private final File wrapperScript = TestUtil.resolveScriptForOS(testDir + "/wrapper");


    // Get suitable exit codes for the OS
    private static int SUCCESS_STATUS; // test script successful exit code
    private static int ERROR_STATUS;   // test script error exit code

    @BeforeClass
    public static void classSetUp() {

        final int statuses[] = TestUtil.getTestScriptCodesForOS();
        SUCCESS_STATUS=statuses[0];
        ERROR_STATUS=statuses[1];

        // turn on debug mode and throw an exception for each encountered problem
        System.setProperty("org.apache.commons.exec.lenient", "false");
        System.setProperty("org.apache.commons.exec.debug", "true");
    }

    @Before
    public void setUp() throws Exception {

        // delete the marker file
        this.foreverOutputFile.getParentFile().mkdirs();
        if (this.foreverOutputFile.exists()) {
            this.foreverOutputFile.delete();
        }

        // prepare a ready to Executor
        this.baos = new ByteArrayOutputStream();
        this.exec.setStreamHandler(new PumpStreamHandler(baos, baos));
    }

    @After
    public void tearDown() throws Exception {
        this.baos.close();
        foreverOutputFile.delete();
    }

    // ======================================================================
    // Start of regression tests
    // ======================================================================

    /**
     * The simplest possible test - start a script and
     * check that the output was pumped into our
     * <code>ByteArrayOutputStream</code>.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecute() throws Exception {
        final CommandLine cl = new CommandLine(testScript);
        final int exitValue = exec.execute(cl);
        assertEquals("FOO..", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
        assertEquals(new File("."), exec.getWorkingDirectory());
    }

    @Test
    public void testExecuteWithWorkingDirectory() throws Exception {
        final File workingDir = new File("./target");
        final CommandLine cl = new CommandLine(testScript);
        exec.setWorkingDirectory(workingDir);
        final int exitValue = exec.execute(cl);
        assertEquals("FOO..", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
        assertEquals(exec.getWorkingDirectory(), workingDir);
    }

    @Test
    public void testExecuteWithInvalidWorkingDirectory() throws Exception {
        final File workingDir = new File("/foo/bar");
        final CommandLine cl = new CommandLine(testScript);
        exec.setWorkingDirectory(workingDir);
        try {
            exec.execute(cl);
            fail("Expected exception due to invalid working directory");
        }
        catch (final IOException e) {
            return;
        }
    }

    @Test
    public void testExecuteWithError() throws Exception {
        final CommandLine cl = new CommandLine(errorTestScript);

        try{
            exec.execute(cl);
            fail("Must throw ExecuteException");
        } catch (final ExecuteException e) {
            assertTrue(exec.isFailure(e.getExitValue()));
        }
    }

    @Test
    public void testExecuteWithArg() throws Exception {
        final CommandLine cl = new CommandLine(testScript);
        cl.addArgument("BAR");
        final int exitValue = exec.execute(cl);

        assertEquals("FOO..BAR", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
    }

    /**
     * Execute the test script and pass a environment containing
     * 'TEST_ENV_VAR'.
     */
    @Test
    public void testExecuteWithSingleEnvironmentVariable() throws Exception {
        final Map env = new HashMap();
        env.put("TEST_ENV_VAR", "XYZ");

        final CommandLine cl = new CommandLine(testScript);

        final int exitValue = exec.execute(cl, env);

        assertEquals("FOO.XYZ.", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
    }

    /**
     * Start a asynchronous process which returns an success
     * exit value.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteAsync() throws Exception {
        final CommandLine cl = new CommandLine(testScript);
        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        exec.execute(cl, resultHandler);
        resultHandler.waitFor(2000);
        assertTrue(resultHandler.hasResult());
        assertNull(resultHandler.getException());
        assertFalse(exec.isFailure(resultHandler.getExitValue()));
        assertEquals("FOO..", baos.toString().trim());
    }

    /**
     * Start a asynchronous process which returns an error
     * exit value.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteAsyncWithError() throws Exception {
        final CommandLine cl = new CommandLine(errorTestScript);
        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        exec.execute(cl, resultHandler);
        resultHandler.waitFor(2000);
        assertTrue(resultHandler.hasResult());
        assertTrue(exec.isFailure(resultHandler.getExitValue()));
        assertNotNull(resultHandler.getException());
        assertEquals("FOO..", baos.toString().trim());
    }

    /**
     * Start a asynchronous process and terminate it manually before the
     * watchdog timeout occurs.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteAsyncWithTimelyUserTermination() throws Exception {
        final CommandLine cl = new CommandLine(foreverTestScript);
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(Integer.MAX_VALUE);
        exec.setWatchdog(watchdog);
        final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        exec.execute(cl, handler);
        // wait for script to run
        Thread.sleep(2000);
        assertTrue("Watchdog should watch the process", watchdog.isWatching());
        // terminate it manually using the watchdog
        watchdog.destroyProcess();
        // wait until the result of the process execution is propagated
        handler.waitFor(WAITFOR_TIMEOUT);
        assertTrue("Watchdog should have killed the process", watchdog.killedProcess());
        assertFalse("Watchdog is no longer watching the process", watchdog.isWatching());
        assertTrue("ResultHandler received a result", handler.hasResult());
        assertNotNull("ResultHandler received an exception as result", handler.getException());
    }

    /**
     * Start a asynchronous process and try to terminate it manually but
     * the process was already terminated by the watchdog. This is
     * basically a race condition between infrastructure and user
     * code.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteAsyncWithTooLateUserTermination() throws Exception {
        final CommandLine cl = new CommandLine(foreverTestScript);
        final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(3000);
        exec.setWatchdog(watchdog);
        exec.execute(cl, handler);
        // wait for script to be terminated by the watchdog
        Thread.sleep(6000);
        // try to terminate the already terminated process
        watchdog.destroyProcess();
        // wait until the result of the process execution is propagated
        handler.waitFor(WAITFOR_TIMEOUT);
        assertTrue("Watchdog should have killed the process already", watchdog.killedProcess());
        assertFalse("Watchdog is no longer watching the process", watchdog.isWatching());
        assertTrue("ResultHandler received a result", handler.hasResult());
        assertNotNull("ResultHandler received an exception as result", handler.getException());
    }

    /**
     * Start a script looping forever (synchronously) and check if the ExecuteWatchdog
     * kicks in killing the run away process. To make killing a process
     * more testable the "forever" scripts write each second a '.'
     * into "./target/forever.txt" (a marker file). After a test run
     * we should have a few dots in there.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWatchdogSync() throws Exception {

        if (OS.isFamilyOpenVms()) {
            System.out.println("The test 'testExecuteWatchdogSync' currently hangs on the following OS : "
                    + System.getProperty("os.name"));
            return;
        }

        final long timeout = 10000;

        final CommandLine cl = new CommandLine(foreverTestScript);
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File("."));
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        executor.setWatchdog(watchdog);

        try {
            executor.execute(cl);
        }
 catch (final ExecuteException e) {
            Thread.sleep(timeout);
            final int nrOfInvocations = getOccurrences(readFile(this.foreverOutputFile), '.');
            assertTrue(executor.getWatchdog().killedProcess());
            assertTrue("killing the subprocess did not work : " + nrOfInvocations, nrOfInvocations > 5
                    && nrOfInvocations <= 11);
            return;
        }
        catch (final Throwable t) {
            fail(t.getMessage());
        }

        assertTrue("Killed process should be true", executor.getWatchdog().killedProcess() );
        fail("Process did not create ExecuteException when killed");
    }

    /**
     * Start a script looping forever (asynchronously) and check if the
     * ExecuteWatchdog kicks in killing the run away process. To make killing
     * a process more testable the "forever" scripts write each second a '.'
     * into "./target/forever.txt" (a marker file). After a test run
     * we should have a few dots in there.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWatchdogAsync() throws Exception {

        final long timeout = 10000;

        final CommandLine cl = new CommandLine(foreverTestScript);
        final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File("."));
        executor.setWatchdog(new ExecuteWatchdog(timeout));

        executor.execute(cl, handler);
        handler.waitFor(WAITFOR_TIMEOUT);

        assertTrue("Killed process should be true", executor.getWatchdog().killedProcess() );
        assertTrue("ResultHandler received a result", handler.hasResult());
        assertNotNull("ResultHandler received an exception as result", handler.getException());

        final int nrOfInvocations = getOccurrences(readFile(this.foreverOutputFile), '.');
        assertTrue("Killing the process did not work : " + nrOfInvocations, nrOfInvocations > 5 && nrOfInvocations <= 11);
    }

    /**
     * [EXEC-68] Synchronously starts a short script with a Watchdog attached with an extremely large timeout. Checks
     * to see if the script terminated naturally or if it was killed by the Watchdog. Fail if killed by Watchdog.
     * 
     * @throws Exception
     *             the test failed
     */
    @Test
    public void testExecuteWatchdogVeryLongTimeout() throws Exception {
        final long timeout = Long.MAX_VALUE;

        final CommandLine cl = new CommandLine(testScript);
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File("."));
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        executor.setWatchdog(watchdog);

        try {
            executor.execute(cl);
        } catch (final ExecuteException e) {
            assertFalse("Process should exit normally, not be killed by watchdog", watchdog.killedProcess());
            // If the Watchdog did not kill it, something else went wrong.
            throw e;
        }
    }

    /**
     * Try to start an non-existing application which should result
     * in an exception.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteNonExistingApplication() throws Exception {
        final CommandLine cl = new CommandLine(nonExistingTestScript);
        final DefaultExecutor executor = new DefaultExecutor();
        try {
            executor.execute(cl);
        } catch (final IOException e) {
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
    @Test
    public void testExecuteAsyncWithNonExistingApplication() throws Exception {
        final CommandLine cl = new CommandLine(nonExistingTestScript);
        final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
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
    @Test
    public void testExecuteWithCustomExitValue1() throws Exception {
        exec.setExitValue(ERROR_STATUS);
        final CommandLine cl = new CommandLine(errorTestScript);
        exec.execute(cl);
    }

    /**
     * Invoke the error script but define that SUCCESS_STATUS is a bad
     * exit value and therefore an exception should be thrown.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWithCustomExitValue2() throws Exception {
        final CommandLine cl = new CommandLine(errorTestScript);
        exec.setExitValue(SUCCESS_STATUS);
        try{
            exec.execute(cl);
            fail("Must throw ExecuteException");
        } catch (final ExecuteException e) {
            assertTrue(exec.isFailure(e.getExitValue()));
            return;
        }
    }

    /**
     * Test the proper handling of ProcessDestroyer for an synchronous process.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWithProcessDestroyer() throws Exception {

      final CommandLine cl = new CommandLine(testScript);
      final ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();
      exec.setProcessDestroyer(processDestroyer);

      assertTrue(processDestroyer.size() == 0);
      assertTrue(processDestroyer.isAddedAsShutdownHook() == false);

      final int exitValue = exec.execute(cl);

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
    @Test
    public void testExecuteAsyncWithProcessDestroyer() throws Exception {

      final CommandLine cl = new CommandLine(foreverTestScript);
      final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
      final ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();
      final ExecuteWatchdog watchdog = new ExecuteWatchdog(Integer.MAX_VALUE);

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
      handler.waitFor(WAITFOR_TIMEOUT);
      assertTrue("ResultHandler received a result", handler.hasResult());
      assertNotNull(handler.getException());
      assertEquals("Processor Destroyer size should be 0", 0, processDestroyer.size());
      assertFalse("Process destroyer should not exist as shutdown hook", processDestroyer.isAddedAsShutdownHook());
    }

    /**
     * Invoke the test using some fancy arguments.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWithFancyArg() throws Exception {
        final CommandLine cl = new CommandLine(testScript);
        cl.addArgument("test $;`(0)[1]{2}");
        final int exitValue = exec.execute(cl);
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
    @Test
    public void testExecuteWithRedirectedStreams() throws Exception {
        if (OS.isFamilyUnix()) {
            final FileInputStream fis = new FileInputStream("./NOTICE.txt");
            final CommandLine cl = new CommandLine(redirectScript);
            final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(baos, baos, fis);
            final DefaultExecutor executor = new DefaultExecutor();
            executor.setWorkingDirectory(new File("."));
            executor.setStreamHandler(pumpStreamHandler);
            final int exitValue = executor.execute(cl);
            fis.close();
            final String result = baos.toString().trim();
            assertTrue(result, result.indexOf("Finished reading from stdin") > 0);
            assertFalse("exitValue=" + exitValue, exec.isFailure(exitValue));
        } else if (OS.isFamilyWindows()) {
            System.err
                    .println("The code samples to do that in windows look like a joke ... :-( .., no way I'm doing that");
            System.err.println("The test 'testExecuteWithRedirectedStreams' does not support the following OS : "
                    + System.getProperty("os.name"));
            return;
        } else {
            System.err.println("The test 'testExecuteWithRedirectedStreams' does not support the following OS : "
                    + System.getProperty("os.name"));
            return;
        }
    }

     /**
      * Start a process and connect stdout and stderr.
      *
      * @throws Exception the test failed
      */
     @Test
    public void testExecuteWithStdOutErr() throws Exception {
        final CommandLine cl = new CommandLine(testScript);
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(System.out, System.err);
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(pumpStreamHandler);
        final int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
    }

    /**
     * Start a process and connect it to no stream.
     * 
     * @throws Exception
     *             the test failed
     */
    @Test
    public void testExecuteWithNullOutErr() throws Exception {
        final CommandLine cl = new CommandLine(testScript);
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(null, null);
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(pumpStreamHandler);
        final int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
    }

     /**
      * Start a process and connect out and err to a file.
      *
      * @throws Exception the test failed
      */
     @Test
     public void testExecuteWithRedirectOutErr() throws Exception
     {
         final File outfile = File.createTempFile("EXEC", ".test");
         outfile.deleteOnExit();
         final CommandLine cl = new CommandLine(testScript);
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(new FileOutputStream(outfile));
         final DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(pumpStreamHandler);
         final int exitValue = executor.execute(cl);
         assertFalse(exec.isFailure(exitValue));
         assertTrue(outfile.exists());
     }

    /**
     * A generic test case to print the command line arguments to 'printargs' script to solve
     * even more command line puzzles.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWithComplexArguments() throws Exception {
        final CommandLine cl = new CommandLine(printArgsScript);
        cl.addArgument("gdal_translate");
        cl.addArgument("HDF5:\"/home/kk/grass/data/4404.he5\"://HDFEOS/GRIDS/OMI_Column_Amount_O3/Data_Fields/ColumnAmountO3/home/kk/4.tif", false);
        final DefaultExecutor executor = new DefaultExecutor();
        final int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
     }


    /**
     * The test script reads an argument from <code>stdin<code> and prints
     * the result to stdout. To make things slightly more interesting
     * we are using an asynchronous process.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testStdInHandling() throws Exception {
        // newline not needed; causes problems for VMS
        final ByteArrayInputStream bais = new ByteArrayInputStream("Foo".getBytes());
        final CommandLine cl = new CommandLine(this.stdinSript);
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(this.baos, System.err, bais);
        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        final Executor executor = new DefaultExecutor();
        executor.setStreamHandler(pumpStreamHandler);
        executor.execute(cl, resultHandler);

        resultHandler.waitFor(WAITFOR_TIMEOUT);
        assertTrue("ResultHandler received a result", resultHandler.hasResult());

        assertFalse(exec.isFailure(resultHandler.getExitValue()));
        final String result = baos.toString();
        assertTrue("Result '" + result + "' should contain 'Hello Foo!'", result.indexOf("Hello Foo!") >= 0);
    }

    /**
     * Call a script to dump the environment variables of the subprocess.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testEnvironmentVariables() throws Exception {
        exec.execute(new CommandLine(environmentSript));
        final String environment = baos.toString().trim();
        assertTrue("Found no environment variables", environment.length() > 0);
        assertFalse(environment.indexOf("NEW_VAR") >= 0);
    }

    /**
     * Call a script to dump the environment variables of the subprocess
     * after adding a custom environment variable.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testAddEnvironmentVariables() throws Exception {
        final Map myEnvVars = new HashMap();
        myEnvVars.putAll(EnvironmentUtils.getProcEnvironment());
        myEnvVars.put("NEW_VAR","NEW_VAL");
        exec.execute(new CommandLine(environmentSript), myEnvVars);
        final String environment = baos.toString().trim();
        assertTrue("Expecting NEW_VAR in "+environment,environment.indexOf("NEW_VAR") >= 0);
        assertTrue("Expecting NEW_VAL in "+environment,environment.indexOf("NEW_VAL") >= 0);
    }

    @Test
    public void testAddEnvironmentVariableEmbeddedQuote() throws Exception {
        final Map myEnvVars = new HashMap();
        myEnvVars.putAll(EnvironmentUtils.getProcEnvironment());
        final String name = "NEW_VAR";
        final String value = "NEW_\"_VAL";
        myEnvVars.put(name,value);
        exec.execute(new CommandLine(environmentSript), myEnvVars);
        final String environment = baos.toString().trim();
        assertTrue("Expecting "+name+" in "+environment,environment.indexOf(name) >= 0);
        assertTrue("Expecting "+value+" in "+environment,environment.indexOf(value) >= 0);
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
    @Test
    public void testExec33() throws Exception {
        final CommandLine cl = new CommandLine(testScript);
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(System.out, System.err, System.in);
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(pumpStreamHandler);
        final int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
    }

    /**
     * EXEC-34 https://issues.apache.org/jira/browse/EXEC-34
     *
     * Race condition prevent watchdog working using ExecuteStreamHandler.
     * The test fails because when watchdog.destroyProcess() is invoked the
     * external process is not bound to the watchdog yet.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec34_1() throws Exception {

        final CommandLine cmdLine = new CommandLine(pingScript);
        cmdLine.addArgument("10"); // sleep 10 secs

        final ExecuteWatchdog watchdog = new ExecuteWatchdog(Integer.MAX_VALUE);
        final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        exec.setWatchdog(watchdog);
        exec.execute(cmdLine, handler);
        assertTrue(watchdog.isWatching());
        watchdog.destroyProcess();
        assertTrue("Watchdog should have killed the process",watchdog.killedProcess());
        assertFalse("Watchdog is no longer watching the process",watchdog.isWatching());
    }

    /**
     * EXEC-34 https://issues.apache.org/jira/browse/EXEC-34
     *
     * Some user waited for an asynchronous process using watchdog.isWatching() which
     * is now properly implemented  using <code>DefaultExecuteResultHandler</code>.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec34_2() throws Exception {

        final CommandLine cmdLine = new CommandLine(pingScript);
        cmdLine.addArgument("10"); // sleep 10 secs

        final ExecuteWatchdog watchdog = new ExecuteWatchdog(5000);
        final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        exec.setWatchdog(watchdog);
        exec.execute(cmdLine, handler);
        handler.waitFor();
        assertTrue("Process has exited", handler.hasResult());
        assertNotNull("Process was aborted", handler.getException());
        assertTrue("Watchdog should have killed the process", watchdog.killedProcess());
        assertFalse("Watchdog is no longer watching the process", watchdog.isWatching());
    }

    /**
     * Test EXEC-36 see https://issues.apache.org/jira/browse/EXEC-36
     *
     * Original example from Kai Hu which only can be tested on Unix
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec36_1() throws Exception {

        if (OS.isFamilyUnix()) {

            CommandLine cmdl;

            /**
             * ./script/jrake cruise:publish_installers INSTALLER_VERSION=unstable_2_1 \
             *     INSTALLER_PATH="/var/lib/ cruise-agent/installers" INSTALLER_DOWNLOAD_SERVER='something' WITHOUT_HELP_DOC=true
             */

            final String expected = "./script/jrake\n" +
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

            final int exitValue = exec.execute(cmdl);
            final String result = baos.toString().trim();
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
    @Test
    public void testExec36_2() throws Exception {

        String expected;

        // the original command line
        // dotnetfx.exe /q:a /c:"install.exe /l ""\Documents and Settings\myusername\Local Settings\Temp\netfx.log"" /q"

        if (OS.isFamilyWindows()) {
            expected = "dotnetfx.exe\n" +
                "/q:a\n" +
                "/c:\"install.exe /l \"\"\\Documents and Settings\\myusername\\Local Settings\\Temp\\netfx.log\"\" /q\"";
        }
        else if (OS.isFamilyUnix()) {
            expected = "dotnetfx.exe\n" +
                "/q:a\n" +
                "/c:\"install.exe /l \"\"/Documents and Settings/myusername/Local Settings/Temp/netfx.log\"\" /q\"";
        }
        else {
            System.err.println("The test 'testExec36_3' does not support the following OS : " + System.getProperty("os.name"));
            return;
        }

        CommandLine cmdl;
        final File file = new File("/Documents and Settings/myusername/Local Settings/Temp/netfx.log");
        final Map map = new HashMap();
        map.put("FILE", file);

        cmdl = new CommandLine(printArgsScript);
        cmdl.setSubstitutionMap(map);
        cmdl.addArgument("dotnetfx.exe", false);
        cmdl.addArgument("/q:a", false);
        cmdl.addArgument("/c:\"install.exe /l \"\"${FILE}\"\" /q\"", false);

        final int exitValue = exec.execute(cmdl);
        final String result = baos.toString().trim();
        assertFalse(exec.isFailure(exitValue));

        if (OS.isFamilyUnix()) {
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
     * @throws Exception the test failed
     */
    @Test
    public void testExec41WithStreams() throws Exception {

        CommandLine cmdLine;

        if (OS.isFamilyWindows()) {
            cmdLine = CommandLine.parse("ping.exe -n 10 -w 1000 127.0.0.1");
        } else if ("HP-UX".equals(System.getProperty("os.name"))) {
            // see EXEC-52 - option must appear after the hostname!
            cmdLine = CommandLine.parse("ping 127.0.0.1 -n 10");
        } else if (OS.isFamilyUnix()) {
            cmdLine = CommandLine.parse("ping -c 10 127.0.0.1");
        } else {
            System.err.println("The test 'testExec41WithStreams' does not support the following OS : "
                    + System.getProperty("os.name"));
            return;
        }

        final DefaultExecutor executor = new DefaultExecutor();
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(2 * 1000); // allow process no more than 2 secs
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(System.out, System.err);
        // this method was part of the patch I reverted
        // pumpStreamHandler.setAlwaysWaitForStreamThreads(false);

        executor.setWatchdog(watchdog);
        executor.setStreamHandler(pumpStreamHandler);

        final long startTime = System.currentTimeMillis();

        try {
            executor.execute(cmdLine);
        } catch (final ExecuteException e) {
            // nothing to do
        }

        final long duration = System.currentTimeMillis() - startTime;

        System.out.println("Process completed in " + duration + " millis; below is its output");

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
    @Test
    public void testExec41WithoutStreams() throws Exception {

        final CommandLine cmdLine = new CommandLine(pingScript);
        cmdLine.addArgument("10"); // sleep 10 secs
        final DefaultExecutor executor = new DefaultExecutor();
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(2*1000); // allow process no more than 2 secs

        // create a custom "PumpStreamHandler" doing no pumping at all
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(null, null, null);

        executor.setWatchdog(watchdog);
        executor.setStreamHandler(pumpStreamHandler);

        final long startTime = System.currentTimeMillis();

        try {
            executor.execute(cmdLine);
        } catch (final ExecuteException e) {
            System.out.println(e);
        }

        final long duration = System.currentTimeMillis() - startTime;

        System.out.println("Process completed in " + duration +" millis; below is its output");

        if (watchdog.killedProcess()) {
            System.out.println("Process timed out and was killed.");
        }

        assertTrue("The process was killed by the watchdog", watchdog.killedProcess());
        assertTrue("Skipping the Thread.join() did not work, duration="+duration, duration < 9000);
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
    @Test
    public void testExec44() throws Exception {

        final CommandLine cl = new CommandLine(foreverTestScript);
        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);

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
     * Test EXEC-49 (https://issues.apache.org/jira/browse/EXEC-49).
     *
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
                // System.out.println("pis.available() " + pis.available());
                // System.out.println("x " + x);
            }
            pis.close();

            handler.waitFor();
        }
    }

    /**
     * Test EXEC-49 (https://issues.apache.org/jira/browse/EXEC-49).
     *
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
                // System.out.println("pis.available() " + pis.available());
                // System.out.println("x " + x);
            }
            pis.close();

            handler.waitFor();
        }
    }

    /**
     * Test EXEC-57 (https://issues.apache.org/jira/browse/EXEC-57).
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

    /**
     * Test EXEC-60 (https://issues.apache.org/jira/browse/EXEC-60).
     *
     * Possible deadlock when a process is terminating at the same time its timing out. Please
     * note that a successful test is no proof that the issues was indeed fixed.
     *
     */
    @Test
    public void testExec_60() throws Exception {

        final int start = 0;
        final int seconds = 1;
        final int offsetMultiplier = 1;
        final int maxRetries = 180;
        int processTerminatedCounter = 0;
        int watchdogKilledProcessCounter = 0;
        final CommandLine cmdLine = new CommandLine(pingScript);
        cmdLine.addArgument(Integer.toString(seconds + 1)); // need to add "1" to wait the requested number of seconds

        final long startTime = System.currentTimeMillis();
        for (int offset = start; offset <= maxRetries; offset++) {
            // wait progressively longer for process to complete
            // tricky to get this test right. We want to try and catch the process while it is terminating,
            // so we increase the timeout gradually until the test terminates normally.
            // However if the increase is too gradual, we never wait long enough for any test to exit normally
            final ExecuteWatchdog watchdog = new ExecuteWatchdog(seconds * 1000 + offset * offsetMultiplier);
            exec.setWatchdog(watchdog);
            try {
                exec.execute(cmdLine);
                processTerminatedCounter++;
//                System.out.println(offset + ": process has terminated: " + watchdog.killedProcess());
                if (processTerminatedCounter > 5) {
                    break;
                }
            } catch (final ExecuteException ex) {
//                System.out.println(offset + ": process was killed: " + watchdog.killedProcess());
                assertTrue("Watchdog killed the process", watchdog.killedProcess());
                watchdogKilledProcessCounter++;
            }
        }

        final long avg = (System.currentTimeMillis() - startTime) / 
                (watchdogKilledProcessCounter+processTerminatedCounter);
        System.out.println("Processes terminated: "+processTerminatedCounter+" killed: "+watchdogKilledProcessCounter
                +" Multiplier: "+offsetMultiplier+" MaxRetries: "+maxRetries+" Elapsed (avg ms): "+avg);
        assertTrue("Not a single process terminated on its own", processTerminatedCounter > 0);
        assertTrue("Not a single process was killed by the watch dog", watchdogKilledProcessCounter > 0);
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
    @Test
    @Ignore
    public void _testExecuteStability() throws Exception {

        // make a plain-vanilla test
        for (int i=0; i<100; i++) {
            final Map env = new HashMap();
            env.put("TEST_ENV_VAR", new Integer(i));
            final CommandLine cl = new CommandLine(testScript);
            final int exitValue = exec.execute(cl,env);
            assertFalse(exec.isFailure(exitValue));
            assertEquals("FOO." + i + ".", baos.toString().trim());
            baos.reset();
        }

        // now be nasty and use the watchdog to kill out sub-processes
        for (int i=0; i<100; i++) {
            final Map env = new HashMap();
            env.put("TEST_ENV_VAR", new Integer(i));
            final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
            final CommandLine cl = new CommandLine(foreverTestScript);
            final ExecuteWatchdog watchdog = new ExecuteWatchdog(500);
            exec.setWatchdog(watchdog);
            exec.execute(cl, env, resultHandler);
            resultHandler.waitFor(WAITFOR_TIMEOUT);
            assertTrue("ResultHandler received a result", resultHandler.hasResult());
            assertNotNull(resultHandler.getException());
            baos.reset();
        }
    }


    // ======================================================================
    // === Helper methods
    // ======================================================================

    private String readFile(final File file) throws Exception {

        String text;
        final StringBuffer contents = new StringBuffer();
        final BufferedReader reader = new BufferedReader(new FileReader(file));

        while ((text = reader.readLine()) != null)
        {
            contents.append(text)
                .append(System.getProperty(
                    "line.separator"));
        }
        reader.close();
        return contents.toString();
    }

    private int getOccurrences(final String data, final char c) {

        int result = 0;

        for (int i=0; i<data.length(); i++) {
            if (data.charAt(i) == c) {
                result++;
            }
        }

        return result;
    }
}
