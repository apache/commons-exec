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
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.util.StringUtils;

/**
 * A command launcher for VMS that writes the command to a temporary DCL script before launching commands. This is due to limitations of both the DCL
 * interpreter and the Java VM implementation.
 */
public class VmsCommandLauncher extends Java13CommandLauncher {

    /**
     * Constructs a new instance.
     */
    public VmsCommandLauncher() {
        // empty
    }

    /**
     * Writes the command into a temporary DCL script and returns the corresponding File object. The script will be deleted on exit.
     */
    File createCommandFile(final CommandLine cmd, final Map<String, String> env) throws IOException {
        final Path path = Files.createTempFile("EXEC", ".TMP");
        final File script = path.toFile();
        script.deleteOnExit();
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path, Charset.defaultCharset()))) {
            // add the environment as global symbols for the DCL script
            if (env != null) {
                final Set<Entry<String, String>> entries = env.entrySet();
                for (final Entry<String, String> entry : entries) {
                    writer.print("$ ");
                    writer.print(entry.getKey());
                    writer.print(" == "); // define as global symbol
                    writer.println('\"');
                    String value = entry.getValue();
                    // Any embedded " values need to be doubled
                    if (value.indexOf('\"') > 0) {
                        final StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < value.length(); i++) {
                            final char c = value.charAt(i);
                            if (c == '\"') {
                                sb.append('\"');
                            }
                            sb.append(c);
                        }
                        value = sb.toString();
                    }
                    writer.print(value);
                    writer.println('\"');
                }
            }
            final String command = cmd.getExecutable();
            if (cmd.isFile()) {
                // We assume it is it a script file
                writer.print("$ @");
                // This is a bit crude, but seems to work
                final String[] parts = StringUtils.split(command, "/");
                writer.print(parts[0]); // device
                writer.print(":[");
                writer.print(parts[1]); // top level directory
                final int lastPart = parts.length - 1;
                for (int i = 2; i < lastPart; i++) {
                    writer.print(".");
                    writer.print(parts[i]);
                }
                writer.print("]");
                writer.print(parts[lastPart]);
            } else {
                writer.print("$ ");
                writer.print(command);
            }
            final String[] args = cmd.getArguments();
            for (final String arg : args) {
                writer.println(" -");
                writer.print(arg);
            }
            writer.println();
        }
        return script;
    }

    /**
     * Launches the given command in a new process.
     */
    @Override
    public Process exec(final CommandLine cmd, final Map<String, String> env) throws IOException {
        return super.exec(new CommandLine(createCommandFile(cmd, env).getPath()), env);
    }

    /**
     * Launches the given command in a new process, in the given working directory. Note that under Java 1.3.1, 1.4.0 and 1.4.1 on VMS this method only works if
     * {@code workingDir} is {@code null} or the logical JAVA$FORK_SUPPORT_CHDIR needs to be set to TRUE.
     */
    @Override
    public Process exec(final CommandLine cmd, final Map<String, String> env, final File workingDir) throws IOException {
        return super.exec(new CommandLine(createCommandFile(cmd, env).getPath()), env, workingDir);
    }

    /** @see org.apache.commons.exec.launcher.CommandLauncher#isFailure(int) */
    @Override
    public boolean isFailure(final int exitValue) {
        // even exit value signals failure
        return exitValue % 2 == 0;
    }
}
