/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.exec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * The main abstraction to start an external process.
 *
 * The interface allows to:
 * <ul>
 * <li>set a current working directory for the subprocess</li>
 * <li>provide a set of environment variables passed to the subprocess</li>
 * <li>capture the subprocess output of stdout and stderr using an ExecuteStreamHandler</li>
 * <li>kill long-running processes using an ExecuteWatchdog</li>
 * <li>define a set of expected exit values</li>
 * <li>terminate any started processes when the main process is terminating using a ProcessDestroyer</li>
 * </ul>
 * <p>
 * The following example shows the basic usage:
 * </p>
 *
 * <pre>
 * Executor exec = DefaultExecutor.builder().get();
 * CommandLine cl = new CommandLine("ls -l");
 * int exitValue = exec.execute(cl);
 * </pre>
 */

public interface Executor {

    /** Invalid exit code. */
    int INVALID_EXITVALUE = 0xdeadbeef;

    /**
     * Executes a command synchronously. The child process inherits all environment variables of the parent process.
     *
     * @param command the command to execute.
     * @return process exit value.
     * @throws ExecuteException execution of subprocess failed or the subprocess returned an exit value indicating a failure {@link Executor#setExitValue(int)}.
     * @throws IOException      If an I/O error occurs.
     */
    int execute(CommandLine command) throws ExecuteException, IOException;

    /**
     * Executes a command asynchronously. The child process inherits all environment variables of the parent process. Result provided to callback handler.
     *
     * @param command the command to execute.
     * @param handler capture process termination and exit code.
     * @throws ExecuteException execution of subprocess failed.
     * @throws IOException      If an I/O error occurs.
     */
    void execute(CommandLine command, ExecuteResultHandler handler) throws ExecuteException, IOException;

    /**
     * Executes a command synchronously.
     *
     * @param command     the command to execute.
     * @param environment The environment for the new process. If {@code null}, the environment of the current process is used.
     * @return process exit value.
     * @throws ExecuteException execution of subprocess failed or the subprocess returned an exit value indicating a failure {@link Executor#setExitValue(int)}.
     * @throws IOException      If an I/O error occurs.
     */
    int execute(CommandLine command, Map<String, String> environment) throws ExecuteException, IOException;

    /**
     * Executes a command asynchronously. The child process inherits all environment variables of the parent process. Result provided to callback handler.
     *
     * @param command     the command to execute.
     * @param environment The environment for the new process. If {@code null}, the environment of the current process is used.
     * @param handler     capture process termination and exit code.
     * @throws ExecuteException execution of subprocess failed.
     * @throws IOException      If an I/O error occurs.
     */
    void execute(CommandLine command, Map<String, String> environment, ExecuteResultHandler handler) throws ExecuteException, IOException;

    /**
     * Sets the handler for cleanup of started processes if the main process is going to terminate.
     *
     * @return the ProcessDestroyer.
     */
    ProcessDestroyer getProcessDestroyer();

    /**
     * Gets the StreamHandler used for providing input and retrieving the output.
     *
     * @return the StreamHandler.
     */
    ExecuteStreamHandler getStreamHandler();

    /**
     * Gets the watchdog used to kill of processes running, typically, too long time.
     *
     * @return the watchdog.
     */
    ExecuteWatchdog getWatchdog();

    /**
     * Gets the working directory of the created process.
     *
     * @return the working directory.
     */
    File getWorkingDirectory();

    /**
     * Gets the working directory of the created process.
     *
     * @return the working directory.
     * @since 1.5.0
     */
    default Path getWorkingDirectoryPath() {
        return getWorkingDirectory().toPath();
    }

    /**
     * Tests whether {@code exitValue} signals a failure. If no exit values are set than the default conventions of the OS is used. e.g. most OS regard an exit
     * code of '0' as successful execution and everything else as failure.
     *
     * @param exitValue the exit value (return code) to be checked.
     * @return {@code true} if {@code exitValue} signals a failure.
     */
    boolean isFailure(int exitValue);

    /**
     * Sets the {@code exitValue} of the process to be considered successful. If a different exit value is returned by the process then
     * {@link org.apache.commons.exec.Executor#execute(CommandLine)} will throw an {@link org.apache.commons.exec.ExecuteException}.
     *
     * @param value the exit code representing successful execution.
     */
    void setExitValue(int value);

    /**
     * Sets a list of {@code exitValue} of the process to be considered successful. The caller can pass one of the following values.
     * <ul>
     * <li>an array of exit values to be considered successful</li>
     * <li>an empty array for auto-detect of successful exit codes relying on {@link org.apache.commons.exec.Executor#isFailure(int)}</li>
     * <li>{@code null} to indicate to skip checking of exit codes</li>
     * </ul>
     *
     * If an undefined exit value is returned by the process then {@link org.apache.commons.exec.Executor#execute(CommandLine)} will throw an
     * {@link org.apache.commons.exec.ExecuteException}.
     *
     * @param values a list of the exit codes.
     */
    void setExitValues(int[] values);

    /**
     * Sets the handler for cleanup of started processes if the main process is going to terminate.
     *
     * @param processDestroyer the ProcessDestroyer.
     */
    void setProcessDestroyer(ProcessDestroyer processDestroyer);

    /**
     * Sets a custom the StreamHandler used for providing input and retrieving the output. If you don't provide a proper stream handler the executed process
     * might block when writing to stdout and/or stderr (see {@link Process Process}).
     *
     * @param streamHandler the stream handler.
     */
    void setStreamHandler(ExecuteStreamHandler streamHandler);

    /**
     * Sets the watchdog used to kill of processes running, typically, too long time.
     *
     * @param watchDog the watchdog.
     */
    void setWatchdog(ExecuteWatchdog watchDog);

    /**
     * Sets the working directory of the created process. The working directory must exist when you start the process.
     *
     * @param dir the working directory.
     */
    void setWorkingDirectory(File dir);

}
