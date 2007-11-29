/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.exec;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.exec.launcher.CommandLauncher;
import org.apache.commons.exec.launcher.CommandLauncherFactory;

/**
 * 
 */
public class DefaultExecutor implements Executor {

    /** taking care of output and error stream */
    private ExecuteStreamHandler streamHandler;

    /** the working directory of the process */
    private File workingDirectory;

    /** monitoring of long running processes */
    private ExecuteWatchdog watchdog;

    /** the exit values considerd to be successful */
    private int[] exitValues;

    // TODO replace with generic launcher
    private CommandLauncher launcher;

    /**
     * Default Constrctor
     */
    public DefaultExecutor() {
        this.streamHandler = new PumpStreamHandler();
        this.launcher = CommandLauncherFactory.createVMLauncher();
        this.exitValues = new int[0];
    }

    /**
     * @see org.apache.commons.exec.Executor#getStreamHandler()
     */
    public ExecuteStreamHandler getStreamHandler() {
        return streamHandler;
    }

    /**
     * @see org.apache.commons.exec.Executor#setStreamHandler(org.apache.commons.exec.ExecuteStreamHandler)
     */
    public void setStreamHandler(ExecuteStreamHandler streamHandler) {
        this.streamHandler = streamHandler;
    }

    /**
     * @see org.apache.commons.exec.Executor#getWatchdog()
     */
    public ExecuteWatchdog getWatchdog() {
        return watchdog;
    }

    /**
     * @see org.apache.commons.exec.Executor#setWatchdog(org.apache.commons.exec.ExecuteWatchdog)
     */
    public void setWatchdog(ExecuteWatchdog watchDog) {
        this.watchdog = watchDog;
    }

    /**
     * @see org.apache.commons.exec.Executor#getWorkingDirectory()
     */
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * @see org.apache.commons.exec.Executor#setWorkingDirectory(java.io.File)
     */
    public void setWorkingDirectory(File dir) {
        this.workingDirectory = dir;
    }

    /**
     * @see org.apache.commons.exec.Executor#execute(CommandLine)
     */
    public int execute(final CommandLine command) throws ExecuteException,
            IOException {
        return execute(command, (Map) null);
    }

    /**
     * @see org.apache.commons.exec.Executor#execute(CommandLine, java.util.Map)
     */
    public int execute(final CommandLine command, Map environment)
            throws ExecuteException, IOException {

        if (workingDirectory != null && !workingDirectory.exists()) {
            throw new IOException(workingDirectory + " doesn't exist.");
        }
        
        return executeInternal(command, environment, workingDirectory, streamHandler);

    }

    /**
     * @see org.apache.commons.exec.Executor#execute(CommandLine,
     *      org.apache.commons.exec.ExecuteResultHandler)
     */
    public void execute(final CommandLine command, ExecuteResultHandler handler)
            throws ExecuteException, IOException {
        execute(command, null, handler);
    }

    /**
     * @see org.apache.commons.exec.Executor#execute(CommandLine,
     *      java.util.Map, org.apache.commons.exec.ExecuteResultHandler)
     */
    public void execute(final CommandLine command, final Map environment,
            final ExecuteResultHandler handler) throws ExecuteException, IOException {
        if (workingDirectory != null && !workingDirectory.exists()) {
            throw new IOException(workingDirectory + " doesn't exist.");
        }

        new Thread() {

            /* (non-Javadoc)
             * @see java.lang.Thread#run()
             */
            public void run() {
                int exitValue = Executor.INVALID_EXITVALUE;
                try {
                    
                    exitValue = executeInternal(command, environment, workingDirectory, streamHandler);

                    // TODO check exitValue and throw if not OK
                    handler.onProcessComplete(exitValue);
                } catch (ExecuteException e) {
                    handler.onProcessFailed(e);
                } catch(Exception e) {
                    handler.onProcessFailed(new ExecuteException("Execution failed", exitValue, e));
                } finally {
                    // remove the process to the list of those to destroy if the VM
                    // exits
                    //
                    // processDestroyer.remove(process);
                }
            }
        }.start();
    }


    /*
     * Define the exit code of the process to considered
     * successful.
     */
    public void setExitValue( int value ) {
        this.setExitValues(new int[] {value});
    }


    /*
     * Define the exist code of the process to considered
     * successful.
     */
    public void setExitValues( int[] values ) {
        this.exitValues = values;
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
     * Close the streams belonging to the given Process.
     *
     * @param process
     *            the <CODE>Process</CODE>.
     */
    private void closeStreams(final Process process) {
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

    /**
     * Creates a process that runs a command.
     *
     * @param command
     *            the command to run
     * @param env
     *            the environment for the command
     * @param dir
     *            the working directory for the command
     * @return the process started
     * @throws IOException
     *             forwarded from the particular launcher used
     */
    private Process launch(final CommandLine command, final Map env,
            final File dir) throws IOException {
        CommandLauncher launcher = this.launcher;

        if (launcher == null) {
            throw new IllegalStateException("CommandLauncher can not be null");
        }

        if (dir != null && !dir.exists()) {
            throw new IOException(dir + " doesn't exist.");
        }
        return launcher.exec(command, env, dir);
    }
    
    private int executeInternal(final CommandLine command, final Map environment,
            final File dir, final ExecuteStreamHandler streams) throws IOException {

        final Process process = launch(command, environment, dir);

        try {
            streams.setProcessInputStream(process.getOutputStream());
            streams.setProcessOutputStream(process.getInputStream());
            streams.setProcessErrorStream(process.getErrorStream());
        } catch (IOException e) {
            process.destroy();
            throw e;
        }
        streams.start();

        try {
            // add the process to the list of those to destroy if the VM exits
            //
            // processDestroyer.add(process);

            if (watchdog != null) {
                watchdog.start(process);
            }
            int exitValue = Executor.INVALID_EXITVALUE;
            try {
                exitValue = process.waitFor();
            } catch (InterruptedException e) {
                process.destroy();
            }

            if (watchdog != null) {
                watchdog.stop();
            }
            streams.stop();
            closeStreams(process);

            if (watchdog != null) {
                try {
                    watchdog.checkException();
                } catch (Exception e) {
                    // TODO: include cause
                    throw new IOException(e.getMessage());
                }

            }

            if(!this.isSuccess(exitValue)) {
                throw new ExecuteException("Process exited with an error: " + exitValue, exitValue);
            }

            return exitValue;
        } finally {
            // remove the process to the list of those to destroy if the VM
            // exits
            //
            // processDestroyer.remove(process);
        }
    }

    private boolean isSuccess(final int exitValue) {

        if(this.exitValues == null) {
            return true;
        }
        else if(this.exitValues.length == 0) {
            return !isFailure(exitValue);
        }
        else {
            for(int i=0; i<this.exitValues.length; i++) {
                if(this.exitValues[i] == exitValue) {
                    return true;
                }
            }
        }
        return false;
    }
}
