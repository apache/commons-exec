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
     * {@code ByteArrayOutputStream}.
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

    @Test(expected = IOException.class)
    public void testExecuteWithInvalidWorkingDirectory() throws Exception {
        final File workingDir = new File("/foo/bar");
        final CommandLine cl = new CommandLine(testScript);
        exec.setWorkingDirectory(workingDir);

        exec.execute(cl);
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
        final Map<String, String> env = new HashMap<String, String>();
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
    @Test(expected = IOException.class)
    public void testExecuteNonExistingApplication() throws Exception {
        final CommandLine cl = new CommandLine(nonExistingTestScript);
        final DefaultExecutor executor = new DefaultExecutor();

        executor.execute(cl);
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
    public void testExecuteWithRedirectOutErr() throws Exception {
        final File outfile = File.createTempFile("EXEC", ".test");
        outfile.deleteOnExit();
        final CommandLine cl = new CommandLine(testScript);
        final FileOutputStream outAndErr = new FileOutputStream(outfile);
        try {
            final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(outAndErr);
            final DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(pumpStreamHandler);
            final int exitValue = executor.execute(cl);
            assertFalse(exec.isFailure(exitValue));
            assertTrue(outfile.exists());
        } finally {
            outAndErr.close();
        }
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
     * The test script reads an argument from {@code stdin} and prints
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
        final Map<String, String> myEnvVars = new HashMap<String, String>();
        myEnvVars.putAll(EnvironmentUtils.getProcEnvironment());
        myEnvVars.put("NEW_VAR","NEW_VAL");
        exec.execute(new CommandLine(environmentSript), myEnvVars);
        final String environment = baos.toString().trim();
        assertTrue("Expecting NEW_VAR in "+environment,environment.indexOf("NEW_VAR") >= 0);
        assertTrue("Expecting NEW_VAL in "+environment,environment.indexOf("NEW_VAL") >= 0);
    }

    @Test
    public void testAddEnvironmentVariableEmbeddedQuote() throws Exception {
        final Map<String, String> myEnvVars = new HashMap<String, String>();
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
            final Map<String, String> env = new HashMap<String, String>();
            env.put("TEST_ENV_VAR", Integer.toString(i));
            final CommandLine cl = new CommandLine(testScript);
            final int exitValue = exec.execute(cl,env);
            assertFalse(exec.isFailure(exitValue));
            assertEquals("FOO." + i + ".", baos.toString().trim());
            baos.reset();
        }

        // now be nasty and use the watchdog to kill out sub-processes
        for (int i=0; i<100; i++) {
            final Map<String, String> env = new HashMap<String, String>();
            env.put("TEST_ENV_VAR", Integer.toString(i));
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
        final StringBuilder contents = new StringBuilder();
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
