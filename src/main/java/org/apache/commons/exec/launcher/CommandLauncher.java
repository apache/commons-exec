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

package org.apache.commons.exec.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.exec.CommandLine;

/**
 * Abstracts platform-dependent implementations.
 */
public interface CommandLauncher {

    /**
     * Executes the given command in a new process.
     *
     * @param commandLine The command to execute.
     * @param env         The environment for the new process. If null, the environment of the current process is used.
     * @return the newly created process.
     * @throws IOException if attempting to run a command in a specific directory.
     */
    Process exec(CommandLine commandLine, Map<String, String> env) throws IOException;

    /**
     * Executes the given command in a new process, in the given working directory.
     *
     * @param commandLine      The command to execute.
     * @param env              The environment for the new process. If null, the environment of the current process is used.
     * @param workingDirectory The directory to start the command in. If null, the current directory is used.
     * @return the newly created process.
     * @throws IOException if trying to change directory.
     */
    Process exec(CommandLine commandLine, Map<String, String> env, File workingDirectory) throws IOException;

    /**
     * Executes the given command in a new process, in the given working directory.
     *
     * @param commandLine      The command to execute.
     * @param env              The environment for the new process. If null, the environment of the current process is used.
     * @param workingDirectory The directory to start the command in. If null, the current directory is used.
     * @return the newly created process.
     * @throws IOException if trying to change directory.
     * @since 1.5.0
     */
    default Process exec(CommandLine commandLine, Map<String, String> env, Path workingDirectory) throws IOException {
        return exec(commandLine, env, workingDirectory != null ? workingDirectory.toFile() : null);
    }

    /**
     * Tests whether {@code exitValue} signals a failure on the current system (OS specific).
     * <p>
     * <strong>Note</strong> that this method relies on the conventions of the OS, it will return false results if the application you are running doesn't
     * follow these conventions. One notable exception is the Java VM provided by HP for OpenVMS - it will return 0 if successful (like on any other platform),
     * but this signals a failure on OpenVMS. So if you execute a new Java VM on OpenVMS, you cannot trust this method.
     * </p>
     *
     * @param exitValue the exit value (return code) to be checked.
     * @return {@code true} if {@code exitValue} signals a failure.
     */
    boolean isFailure(int exitValue);
}
