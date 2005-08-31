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
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.exec.environment.Environment;
import org.apache.commons.exec.launcher.CommandLauncher;
import org.apache.commons.exec.launcher.CommandLauncherFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Runs an external program.
 */
public class Execute {

    private static Log LOG = LogFactory.getLog(Execute.class);

    /** Invalid exit code. * */
    public static final int INVALID = Integer.MAX_VALUE;

    private CommandLine cmdl = null;

    private Environment environment = null;

    private int exitValue = INVALID;

    private ExecuteStreamHandler streamHandler = new LogStreamHandler(1, 1);

    private ExecuteWatchdog watchdog;

    private File workingDirectory = null;

    private boolean newEnvironment = false;

    private static String userWorkingDirectory = System.getProperty("user.dir");

    private static CommandLauncher vmLauncher = CommandLauncherFactory
            .createVMLauncher();

    /** Used to destroy processes when the VM exits. */
    private static ProcessDestroyer processDestroyer = new ProcessDestroyer();

    /**
     * ByteArrayOutputStream#toString doesn't seem to work reliably on OS/390,
     * at least not the way we use it in the execution context.
     * 
     * @param bos
     *            the output stream that one wants to read
     * @return the output stream as a string, read with special encodings in the
     *         case of z/os and os/400
     */
    public static String toString(final ByteArrayOutputStream bos) {
        if (OS.isFamilyZOS()) {
            try {
                return bos.toString("Cp1047");
            } catch (java.io.UnsupportedEncodingException e) {
                // no-op default encoding used
            }
        } else if (OS.isFamilyOS400()) {
            try {
                return bos.toString("Cp500");
            } catch (java.io.UnsupportedEncodingException e) {
                // no-op default encoding used
            }
        }
        return bos.toString();
    }

    /**
     * Creates a new execute object using <code>PumpStreamHandler</code> for
     * stream handling.
     */
    public Execute() {
        this(new PumpStreamHandler(), null);
    }

    /**
     * Creates a new execute object.
     * 
     * @param streamHandler
     *            the stream handler used to handle the input and output streams
     *            of the subprocess.
     */
    public Execute(final ExecuteStreamHandler streamHandler) {
        this(streamHandler, null);
    }

    /**
     * Creates a new execute object.
     * 
     * @param streamHandler
     *            the stream handler used to handle the input and output streams
     *            of the subprocess.
     * @param watchdog
     *            a watchdog for the subprocess or <code>null</code> to to
     *            disable a timeout for the subprocess.
     */
    public Execute(final ExecuteStreamHandler streamHandler,
            final ExecuteWatchdog watchdog) {
        setStreamHandler(streamHandler);
        this.watchdog = watchdog;
    }

    public void setStreamHandler(final ExecuteStreamHandler streamHandler) {
        this.streamHandler = streamHandler;
    }

    /**
     * Returns the command line used to create a subprocess.
     * 
     * @return the command line used to create a subprocess
     */
    public CommandLine getCommandline() {
        return cmdl;
    }

    /**
     * Sets the command line of the subprocess to launch.
     * 
     * @param commandline
     *            the command line of the subprocess to launch
     */
    public void setCommandline(final CommandLine commandline) {
        cmdl = commandline;
    }

    /**
     * Set whether to propagate the default environment or not.
     * 
     * @param newenv
     *            whether to propagate the process environment.
     */
    public void setNewEnvironment(final boolean newenv) {
        newEnvironment = newenv;
    }

    /**
     * Returns the environment used to create a subprocess.
     * 
     * @return the environment used to create a subprocess
     * @throws IOException If the environment can not be
     * 	retrived.
     */
    public Environment getEnvironment() throws IOException {
        if (environment == null || newEnvironment) {
            return environment;
        }
        return patchEnvironment();
    }

    /**
     * Sets the environment variables for the subprocess to launch.
     * 
     * @param envVars
     *            array of Strings, each element of which has an environment
     *            variable settings in format <em>key=value</em>
     */
    public void setEnvironment(final String[] envVars) {
        this.environment = Environment.createEnvironment(envVars);
    }

    /**
     * Sets the environment variables for the subprocess to launch.
     * 
     * @param env
     *            environment to set
     */
    public void setEnvironment(final Environment env) {
        this.environment = env;
    }

    /**
     * Sets the working directory of the process to execute.
     * <p>
     * This is emulated using the antRun scripts unless the OS is Windows NT in
     * which case a cmd.exe is spawned, or MRJ and setting user.dir works, or
     * JDK 1.3 and there is official support in java.lang.Runtime.
     * 
     * @param wd
     *            the working directory of the process.
     */
    public void setWorkingDirectory(final File wd) {
        if (wd == null || wd.getAbsolutePath().equals(userWorkingDirectory)) {
            workingDirectory = null;
        } else {
            workingDirectory = wd;
        }
    }

    /**
     * Creates a process that runs a command.
     * 
     * @param command
     *            the command to run
     * @param env
     *            the environment for the command
     * @param dir
     *            the working directory for the command
     * @param useVM
     *            use the built-in exec command for JDK 1.3 if available.
     * @return the process started
     * @throws IOException
     *             forwarded from the particular launcher used
     */
    public static Process launch(final CommandLine command,
            final Environment env, final File dir)
            throws IOException {
        CommandLauncher launcher = vmLauncher;

        if (dir != null && !dir.exists()) {
            throw new IOException(dir + " doesn't exist.");
        }
        return launcher.exec(command, env, dir);
    }

    /**
     * Runs a process defined by the command line and returns its exit status.
     * 
     * @return the exit status of the subprocess or <code>INVALID</code>
     * @exception java.io.IOException
     *                The exception is thrown, if launching of the subprocess
     *                failed
     */
    public int execute() throws IOException {
        if (workingDirectory != null && !workingDirectory.exists()) {
            throw new IOException(workingDirectory + " doesn't exist.");
        }

        final Process process = launch(getCommandline(), getEnvironment(),
                workingDirectory);

        try {
            streamHandler.setProcessInputStream(process.getOutputStream());
            streamHandler.setProcessOutputStream(process.getInputStream());
            streamHandler.setProcessErrorStream(process.getErrorStream());
        } catch (IOException e) {
            process.destroy();
            throw e;
        }
        streamHandler.start();

        try {
            // add the process to the list of those to destroy if the VM exits
            //
            processDestroyer.add(process);

            if (watchdog != null) {
                watchdog.start(process);
            }
            waitFor(process);

            if (watchdog != null) {
                watchdog.stop();
            }
            streamHandler.stop();
            closeStreams(process);

            if (watchdog != null) {
                watchdog.checkException();
            }
            return getExitValue();
        } finally {
            // remove the process to the list of those to destroy if the VM
            // exits
            //
            processDestroyer.remove(process);
        }
    }

    /**
     * Starts a process defined by the command line. Ant will not wait for this
     * process, nor log its output
     * 
     * @throws java.io.IOException
     *             The exception is thrown, if launching of the subprocess
     *             failed
     */
    public void spawn() throws IOException {
        if (workingDirectory != null && !workingDirectory.exists()) {
            throw new ExecuteException(workingDirectory + " doesn't exist.");
        }
        final Process process = launch(getCommandline(), getEnvironment(),
                workingDirectory);
        if (OS.isFamilyWindows()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.debug("interruption in the sleep after "
                        + "having spawned a process");
            }
        }

        OutputStream dummyOut = new OutputStream() {
            public void write(final int b) throws IOException {
            }
        };

        ExecuteStreamHandler streamHandler = new PumpStreamHandler(dummyOut);
        streamHandler.setProcessErrorStream(process.getErrorStream());
        streamHandler.setProcessOutputStream(process.getInputStream());
        streamHandler.start();
        process.getOutputStream().close();

        LOG.debug("spawned process " + process.toString());
    }

    /**
     * Wait for a given process.
     * 
     * @param process
     *            the process one wants to wait for
     */
    protected void waitFor(final Process process) {
        try {
            process.waitFor();
            setExitValue(process.exitValue());
        } catch (InterruptedException e) {
            process.destroy();
        }
    }

    /**
     * Set the exit value.
     * 
     * @param value
     *            exit value of the process
     */
    protected void setExitValue(final int value) {
        exitValue = value;
    }

    /**
     * Query the exit value of the process.
     * 
     * @return the exit value or Execute.INVALID if no exit value has been
     *         received
     */
    public int getExitValue() {
        return exitValue;
    }

    /**
     * Checks whether <code>exitValue</code> signals a failure on the current
     * system (OS specific).
     * <p>
     * <b>Note</b> that this method relies on the conventions of the OS, it
     * will return false results if the application you are running doesn't
     * follow these conventions. One notable exception is the Java VM provided
     * by HP for OpenVMS - it will return 0 if successful (like on any other
     * platform), but this signals a failure on OpenVMS. So if you execute a new
     * Java VM on OpenVMS, you cannot trust this method.
     * </p>
     * 
     * @param exitValue
     *            the exit value (return code) to be checked
     * @return <code>true</code> if <code>exitValue</code> signals a failure
     */
    public static boolean isFailure(final int exitValue) {
        if (OS.isFamilyOpenVms()) {
            // even exit value signals failure
            return (exitValue % 2) == 0;
        } else {
            // non zero exit value signals failure
            return exitValue != 0;
        }
    }

    /**
     * Test for an untimely death of the process.
     * 
     * @return true if a watchdog had to kill the process
     */
    public boolean killedProcess() {
        return watchdog != null && watchdog.killedProcess();
    }

    /**
     * Patch the current environment with the new values from the user.
     * 
     * @return the patched environment
     * @throws IOException if the procssing environment can not be retrived
     */
    private Environment patchEnvironment() throws IOException {
        // On OpenVMS Runtime#exec() doesn't support the environment array,
        // so we only return the new values which then will be set in
        // the generated DCL script, inheriting the parent process environment
        if (OS.isFamilyOpenVms()) {
            return environment;
        }

        Environment procEnv = Environment.getProcEnvironment();
        Environment osEnv = (Environment) procEnv.clone();

        osEnv.putAll(environment);

        return osEnv;
    }

    /**
     * A utility method that runs an external command. Writes the output and
     * error streams of the command to the project log.
     * 
     * @param cmdline
     *            The command to execute.
     * @throws ExecuteException
     *             if the command does not return 0.
     */
    public static void runCommand(final CommandLine cmdline)
            throws ExecuteException {
        try {
            LOG.debug(cmdline);
            Execute exe = new Execute(new LogStreamHandler(999, 999));

            exe.setCommandline(cmdline);
            int retval = exe.execute();
            if (isFailure(retval)) {
                throw new ExecuteException(cmdline.getExecutable()
                        + " failed with return code " + retval);
            }
        } catch (java.io.IOException exc) {
            throw new ExecuteException("Could not launch "
                    + cmdline.getExecutable() + ": " + exc);
        }
    }

    /**
     * Close the streams belonging to the given Process.
     * 
     * @param process
     *            the <CODE>Process</CODE>.
     */
    public static void closeStreams(final Process process) {
        try {
            process.getInputStream().close();
        } catch (IOException eyeOhEx) {
            // ignore error
        }
        try {
            process.getOutputStream().close();
        } catch (IOException eyeOhEx) {
            // ignore error
        }
        try {
            process.getErrorStream().close();
        } catch (IOException eyeOhEx) {
            // ignore error
        }
    }
}
