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
 */

package org.apache.commons.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.environment.EnvironmentUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junitpioneer.jupiter.SetSystemProperty;

/**
 */
//turn on debug mode and throw an exception for each encountered problem
@SetSystemProperty(key = "org.apache.commons.exec.lenient", value = "false")
@SetSystemProperty(key = "org.apache.commons.exec.debug", value = "true")
public class DefaultExecutorTest {

    /** Maximum time to wait (15s) */
    private static final int WAITFOR_TIMEOUT = 15000;
    private static final Duration WAITFOR_TIMEOUT_D = Duration.ofMillis(WAITFOR_TIMEOUT);

    // Get suitable exit codes for the OS
    private static int SUCCESS_STATUS; // test script successful exit code
    private static int ERROR_STATUS; // test script error exit code

    @BeforeAll
    public static void classSetUp() {
        final int[] statuses = TestUtil.getTestScriptCodesForOS();
        SUCCESS_STATUS = statuses[0];
        ERROR_STATUS = statuses[1];
    }

    private final Executor exec = DefaultExecutor.builder().get();

    private final File testDir = new File("src/test/scripts");
    private final File foreverOutputFile = new File("./target/forever.txt");
    private ByteArrayOutputStream baos;
    private final File testScript = TestUtil.resolveScriptForOS(testDir + "/test");
    private final File errorTestScript = TestUtil.resolveScriptForOS(testDir + "/error");
    private final File foreverTestScript = TestUtil.resolveScriptForOS(testDir + "/forever");
    private final File nonExistingTestScript = TestUtil.resolveScriptForOS(testDir + "/grmpffffff");
    private final File redirectScript = TestUtil.resolveScriptForOS(testDir + "/redirect");

    private final File printArgsScript = TestUtil.resolveScriptForOS(testDir + "/printargs");
    // private final File acroRd32Script = TestUtil.resolveScriptForOS(testDir + "/acrord32");
    private final File stdinSript = TestUtil.resolveScriptForOS(testDir + "/stdin");

    private final File environmentSript = TestUtil.resolveScriptForOS(testDir + "/environment");
//    private final File wrapperScript = TestUtil.resolveScriptForOS(testDir + "/wrapper");

    /**
     * Start any processes in a loop to make sure that we do not leave any handles/resources open.
     *
     * @throws Exception the test failed
     */
    @Test
    @Disabled
    public void _testExecuteStability() throws Exception {

        // make a plain-vanilla test
        for (int i = 0; i < 100; i++) {
            final Map<String, String> env = new HashMap<>();
            env.put("TEST_ENV_VAR", Integer.toString(i));
            final CommandLine cl = new CommandLine(testScript);
            final int exitValue = exec.execute(cl, env);
            assertFalse(exec.isFailure(exitValue));
            assertEquals("FOO." + i + ".", baos.toString().trim());
            baos.reset();
        }

        // now be nasty and use the watchdog to kill out sub-processes
        for (int i = 0; i < 100; i++) {
            final Map<String, String> env = new HashMap<>();
            env.put("TEST_ENV_VAR", Integer.toString(i));
            final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
            final CommandLine cl = new CommandLine(foreverTestScript);
            final ExecuteWatchdog watchdog = new ExecuteWatchdog(500);
            exec.setWatchdog(watchdog);
            exec.execute(cl, env, resultHandler);
            resultHandler.waitFor(WAITFOR_TIMEOUT);
            assertTrue(resultHandler.hasResult(), "ResultHandler received a result");
            assertNotNull(resultHandler.getException());
            baos.reset();
        }
    }

    private int getOccurrences(final String data, final char c) {

        int result = 0;

        for (int i = 0; i < data.length(); i++) {
            if (data.charAt(i) == c) {
                result++;
            }
        }

        return result;
    }

    private String readFile(final File file) throws Exception {
        String text;
        final StringBuilder contents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while ((text = reader.readLine()) != null) {
                contents.append(text).append(System.lineSeparator());
            }
        }
        return contents.toString();
    }

    @BeforeEach
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

    @AfterEach
    public void tearDown() throws Exception {
        this.baos.close();
        foreverOutputFile.delete();
    }

    @Test
    public void testAddEnvironmentVariableEmbeddedQuote() throws Exception {
        final Map<String, String> myEnvVars = new HashMap<>(EnvironmentUtils.getProcEnvironment());
        final String name = "NEW_VAR";
        final String value = "NEW_\"_VAL";
        myEnvVars.put(name, value);
        exec.execute(new CommandLine(environmentSript), myEnvVars);
        final String environment = baos.toString().trim();
        assertTrue(environment.contains(name), () -> "Expecting " + name + " in " + environment);
        assertTrue(environment.contains(value), () -> "Expecting " + value + " in " + environment);
    }

    /**
     * Call a script to dump the environment variables of the subprocess after adding a custom environment variable.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testAddEnvironmentVariables() throws Exception {
        final Map<String, String> myEnvVars = new HashMap<>(EnvironmentUtils.getProcEnvironment());
        myEnvVars.put("NEW_VAR", "NEW_VAL");
        exec.execute(new CommandLine(environmentSript), myEnvVars);
        final String environment = baos.toString().trim();
        assertTrue(environment.contains("NEW_VAR"), () -> "Expecting NEW_VAR in " + environment);
        assertTrue(environment.contains("NEW_VAL"), () -> "Expecting NEW_VAL in " + environment);
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
        assertFalse(environment.isEmpty(), "Found no environment variables");
        assertFalse(environment.contains("NEW_VAR"));
    }

    /**
     * The simplest possible test - start a script and check that the output was pumped into our {@code ByteArrayOutputStream}.
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

    /**
     * Start a asynchronous process which returns an success exit value.
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
     * Try to start an non-existing application where the exception is caught/processed by the result handler.
     */
    @Test
    public void testExecuteAsyncNonExistingApplication() throws Exception {
        final CommandLine cl = new CommandLine(nonExistingTestScript);
        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        final DefaultExecutor executor = DefaultExecutor.builder().get();

        executor.execute(cl, resultHandler);
        resultHandler.waitFor();

        assertTrue(executor.isFailure(resultHandler.getExitValue()));
        assertNotNull(resultHandler.getException());
    }

    /**
     * Try to start an non-existing application where the exception is caught/processed by the result handler. The watchdog in notified to avoid waiting for the
     * process infinitely.
     *
     * @see <a href="https://issues.apache.org/jira/browse/EXEC-71">EXEC-71</a>
     */
    @Test
    public void testExecuteAsyncNonExistingApplicationWithWatchdog() throws Exception {
        final CommandLine cl = new CommandLine(nonExistingTestScript);
        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler() {
            @Override
            public void onProcessFailed(final ExecuteException e) {
                System.out.println("Process did not stop gracefully, had exception '" + e.getMessage() + "' while executing process");
                super.onProcessFailed(e);
            }
        };
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));

        executor.execute(cl, resultHandler);
        resultHandler.waitFor();

        assertTrue(executor.isFailure(resultHandler.getExitValue()));
        assertNotNull(resultHandler.getException());
        assertFalse(executor.getWatchdog().isWatching());
        assertFalse(executor.getWatchdog().killedProcess());
        executor.getWatchdog().destroyProcess();
    }

    /**
     * Start a asynchronous process which returns an error exit value.
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
     * Test the proper handling of ProcessDestroyer for an asynchronous process. Since we do not terminate the process it will be terminated in the
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

        assertNull(exec.getProcessDestroyer());
        assertTrue(processDestroyer.isEmpty());
        assertFalse(processDestroyer.isAddedAsShutdownHook());

        exec.setWatchdog(watchdog);
        exec.setProcessDestroyer(processDestroyer);
        exec.execute(cl, handler);

        // wait for script to start
        Thread.sleep(2000);

        // our process destroyer should be initialized now
        assertNotNull(exec.getProcessDestroyer(), "Process destroyer should exist");
        assertEquals(1, processDestroyer.size(), "Process destroyer size should be 1");
        assertTrue(processDestroyer.isAddedAsShutdownHook(), "Process destroyer should exist as shutdown hook");

        // terminate it and the process destroyer is detached
        watchdog.destroyProcess();
        assertTrue(watchdog.killedProcess());
        handler.waitFor(WAITFOR_TIMEOUT);
        assertTrue(handler.hasResult(), "ResultHandler received a result");
        assertNotNull(handler.getException());
        assertEquals(0, processDestroyer.size(), "Processor Destroyer size should be 0");
        assertFalse(processDestroyer.isAddedAsShutdownHook(), "Process destroyer should not exist as shutdown hook");
    }

    /**
     * Start a asynchronous process and terminate it manually before the watchdog timeout occurs.
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
        assertTrue(watchdog.isWatching(), "Watchdog should watch the process");
        // terminate it manually using the watchdog
        watchdog.destroyProcess();
        // wait until the result of the process execution is propagated
        handler.waitFor(WAITFOR_TIMEOUT);
        assertTrue(watchdog.killedProcess(), "Watchdog should have killed the process");
        assertFalse(watchdog.isWatching(), "Watchdog is no longer watching the process");
        assertTrue(handler.hasResult(), "ResultHandler received a result");
        assertNotNull(handler.getException(), "ResultHandler received an exception as result");
    }

    /**
     * Start a asynchronous process and try to terminate it manually but the process was already terminated by the watchdog. This is basically a race condition
     * between infrastructure and user code.
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
        assertTrue(watchdog.killedProcess(), "Watchdog should have killed the process already");
        assertFalse(watchdog.isWatching(), "Watchdog is no longer watching the process");
        assertTrue(handler.hasResult(), "ResultHandler received a result");
        assertNotNull(handler.getException(), "ResultHandler received an exception as result");
    }

    /**
     * Try to start an non-existing application which should result in an exception.
     */
    @Test
    public void testExecuteNonExistingApplication() throws Exception {
        final CommandLine cl = new CommandLine(nonExistingTestScript);
        final DefaultExecutor executor = DefaultExecutor.builder().get();

        assertThrows(IOException.class, () -> executor.execute(cl));
    }

    /**
     * Try to start an non-existing application which should result in an exception.
     */
    @Test
    public void testExecuteNonExistingApplicationWithWatchDog() throws Exception {
        final CommandLine cl = new CommandLine(nonExistingTestScript);
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));

        assertThrows(IOException.class, () -> executor.execute(cl));
    }

    /**
     * Start a script looping forever (asynchronously) and check if the ExecuteWatchdog kicks in killing the run away process. To make killing a process more
     * testable the "forever" scripts write each second a '.' into "./target/forever.txt" (a marker file). After a test run we should have a few dots in there.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWatchdogAsync() throws Exception {

        final long timeout = 10000;

        final CommandLine cl = new CommandLine(foreverTestScript);
        final DefaultExecuteResultHandler handler = new DefaultExecuteResultHandler();
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.setWorkingDirectory(new File("."));
        executor.setWatchdog(new ExecuteWatchdog(timeout));

        executor.execute(cl, handler);
        handler.waitFor(WAITFOR_TIMEOUT);

        assertTrue(executor.getWatchdog().killedProcess(), "Killed process should be true");
        assertTrue(handler.hasResult(), "ResultHandler received a result");
        assertNotNull(handler.getException(), "ResultHandler received an exception as result");

        final int nrOfInvocations = getOccurrences(readFile(this.foreverOutputFile), '.');
        assertTrue(nrOfInvocations > 5 && nrOfInvocations <= 11, () -> "Killing the process did not work : " + nrOfInvocations);
    }

    /**
     * Start a script looping forever (synchronously) and check if the ExecuteWatchdog kicks in killing the run away process. To make killing a process more
     * testable the "forever" scripts write each second a '.' into "./target/forever.txt" (a marker file). After a test run we should have a few dots in there.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWatchdogSync() throws Exception {

        if (OS.isFamilyOpenVms()) {
            System.out.println("The test 'testExecuteWatchdogSync' currently hangs on the following OS : " + System.getProperty("os.name"));
            return;
        }

        final long timeout = 10000;

        final CommandLine cl = new CommandLine(foreverTestScript);
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.setWorkingDirectory(new File("."));
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        executor.setWatchdog(watchdog);

        try {
            executor.execute(cl);
        } catch (final ExecuteException e) {
            Thread.sleep(timeout);
            final int nrOfInvocations = getOccurrences(readFile(this.foreverOutputFile), '.');
            assertTrue(executor.getWatchdog().killedProcess());
            assertTrue(nrOfInvocations > 5 && nrOfInvocations <= 11, () -> "killing the subprocess did not work : " + nrOfInvocations);
            return;
        } catch (final Throwable t) {
            fail(t.getMessage());
        }

        assertTrue(executor.getWatchdog().killedProcess(), "Killed process should be true");
        fail("Process did not create ExecuteException when killed");
    }

    /**
     * [EXEC-68] Synchronously starts a short script with a Watchdog attached with an extremely large timeout. Checks to see if the script terminated naturally
     * or if it was killed by the Watchdog. Fail if killed by Watchdog.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWatchdogVeryLongTimeout() throws Exception {
        final long timeout = Long.MAX_VALUE;

        final CommandLine cl = new CommandLine(testScript);
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.setWorkingDirectory(new File("."));
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        executor.setWatchdog(watchdog);

        try {
            executor.execute(cl);
        } catch (final ExecuteException e) {
            assertFalse(watchdog.killedProcess(), "Process should exit normally, not be killed by watchdog");
            // If the Watchdog did not kill it, something else went wrong.
            throw e;
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
     * A generic test case to print the command line arguments to 'printargs' script to solve even more command line puzzles.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWithComplexArguments() throws Exception {
        final CommandLine cl = new CommandLine(printArgsScript);
        cl.addArgument("gdal_translate");
        cl.addArgument("HDF5:\"/home/kk/grass/data/4404.he5\"://HDFEOS/GRIDS/OMI_Column_Amount_O3/Data_Fields/ColumnAmountO3/home/kk/4.tif", false);
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        final int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
    }

    /**
     * Invoke the error script but define that the ERROR_STATUS is a good exit value and therefore no exception should be thrown.
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
     * Invoke the error script but define that SUCCESS_STATUS is a bad exit value and therefore an exception should be thrown.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWithCustomExitValue2() throws Exception {
        final CommandLine cl = new CommandLine(errorTestScript);
        exec.setExitValue(SUCCESS_STATUS);
        try {
            exec.execute(cl);
            fail("Must throw ExecuteException");
        } catch (final ExecuteException e) {
            assertTrue(exec.isFailure(e.getExitValue()));
        }
    }

    @Test
    public void testExecuteWithError() throws Exception {
        final CommandLine cl = new CommandLine(errorTestScript);

        try {
            exec.execute(cl);
            fail("Must throw ExecuteException");
        } catch (final ExecuteException e) {
            assertTrue(exec.isFailure(e.getExitValue()));
        }
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

    @Test
    public void testExecuteWithInvalidWorkingDirectory() throws Exception {
        final File workingDir = new File("/foo/bar");
        final CommandLine cl = new CommandLine(testScript);
        exec.setWorkingDirectory(workingDir);

        assertThrows(IOException.class, () -> exec.execute(cl));
    }

    /**
     * Start a process and connect it to no stream.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWithNullOutErr() throws Exception {
        final CommandLine cl = new CommandLine(testScript);
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(null, null);
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.setStreamHandler(pumpStreamHandler);
        final int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
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

        assertTrue(processDestroyer.isEmpty());
        assertFalse(processDestroyer.isAddedAsShutdownHook());

        final int exitValue = exec.execute(cl);

        assertEquals("FOO..", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
        assertTrue(processDestroyer.isEmpty());
        assertFalse(processDestroyer.isAddedAsShutdownHook());
    }

    /**
     * Start a process with redirected streams - stdin of the newly created process is connected to a FileInputStream whereas the "redirect" script reads all
     * lines from stdin and prints them on stdout. Furthermore the script prints a status message on stderr.
     *
     * @throws Exception the test failed
     */
    @Test
    @DisabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
    public void testExecuteWithRedirectedStreams() throws Exception {
        final int exitValue;
        try (FileInputStream fis = new FileInputStream("./NOTICE.txt")) {
            final CommandLine cl = new CommandLine(redirectScript);
            final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(baos, baos, fis);
            final DefaultExecutor executor = DefaultExecutor.builder().get();
            executor.setWorkingDirectory(new File("."));
            executor.setStreamHandler(pumpStreamHandler);
            exitValue = executor.execute(cl);
        }
        final String result = baos.toString().trim();
        assertTrue(result.indexOf("Finished reading from stdin") > 0, result);
        assertFalse(exec.isFailure(exitValue), () -> "exitValue=" + exitValue);
    }

    /**
     * Start a process and connect out and err to a file.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWithRedirectOutErr() throws Exception {
        final Path outFile = Files.createTempFile("EXEC", ".test");
        final CommandLine cl = new CommandLine(testScript);
        try (OutputStream outAndErr = Files.newOutputStream(outFile)) {
            final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(outAndErr);
            final DefaultExecutor executor = DefaultExecutor.builder().get();
            executor.setStreamHandler(pumpStreamHandler);
            final int exitValue = executor.execute(cl);
            assertFalse(exec.isFailure(exitValue));
            assertTrue(Files.exists(outFile));
        } finally {
            Files.delete(outFile);
        }
    }

    /**
     * Execute the test script and pass a environment containing 'TEST_ENV_VAR'.
     */
    @Test
    public void testExecuteWithSingleEnvironmentVariable() throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put("TEST_ENV_VAR", "XYZ");

        final CommandLine cl = new CommandLine(testScript);

        final int exitValue = exec.execute(cl, env);

        assertEquals("FOO.XYZ.", baos.toString().trim());
        assertFalse(exec.isFailure(exitValue));
    }

    // ======================================================================
    // === Long running tests
    // ======================================================================

    /**
     * Start a process and connect stdout and stderr.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExecuteWithStdOutErr() throws Exception {
        final CommandLine cl = new CommandLine(testScript);
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(System.out, System.err);
        final DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.setStreamHandler(pumpStreamHandler);
        final int exitValue = executor.execute(cl);
        assertFalse(exec.isFailure(exitValue));
    }

    // ======================================================================
    // === Helper methods
    // ======================================================================

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

    /**
     * The test script reads an argument from {@code stdin} and prints the result to stdout. To make things slightly more interesting we are using an
     * asynchronous process.
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
        final Executor executor = DefaultExecutor.builder().get();
        executor.setStreamHandler(pumpStreamHandler);
        executor.execute(cl, resultHandler);

        resultHandler.waitFor(WAITFOR_TIMEOUT);
        assertTrue(resultHandler.hasResult(), "ResultHandler received a result");

        assertFalse(exec.isFailure(resultHandler.getExitValue()));
        final String result = baos.toString();
        assertTrue(result.contains("Hello Foo!"), "Result '" + result + "' should contain 'Hello Foo!'");
    }
}
