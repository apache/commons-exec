/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.exec.environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.OS;

/**
 * Helper class to determine the environment variable for the OS. Depending on the JDK the environment variables can be either retrieved directly from the JVM
 * or requires starting a process to get them running an OS command line.
 */
public class DefaultProcessingEnvironment {

    /** The environment variables of the process */
    protected Map<String, String> procEnvironment;

    /**
     * Creates a map that obeys the casing rules of the current platform for key lookup. E.g. on a Windows platform, the map keys will be case-insensitive.
     *
     * @return The map for storage of environment variables, never {@code null}.
     */
    private Map<String, String> createEnvironmentMap() {
        if (OS.isFamilyWindows()) {
            return new TreeMap<>(String::compareToIgnoreCase);
        }
        return new HashMap<>();
    }

    /**
     * Creates the list of environment variables for this process.
     *
     * @return a amp containing the environment variables.
     * @throws IOException the operation failed.
     */
    protected Map<String, String> createProcEnvironment() throws IOException {
        if (procEnvironment == null) {
            procEnvironment = createEnvironmentMap();
            procEnvironment.putAll(System.getenv());
        }
        return procEnvironment;
    }

    /**
     * Determine the OS specific command line to get a list of environment variables.
     *
     * @return the command line.
     * @deprecated No longer needed.
     */
    @Deprecated
    protected CommandLine getProcEnvCommand() {
//        String executable;
//        String[] arguments = null;
//        if (OS.isFamilyOS2()) {
//            // OS/2 - use same mechanism as Windows 2000
//            executable = "cmd";
//
//            arguments = new String[] {"/c", "set"};
//        } else if (OS.isFamilyWindows()) {
//            // Determine if we're running under XP/2000/NT or 98/95
//            if (OS.isFamilyWin9x()) {
//                executable = "command.com";
//                // Windows 98/95
//            } else {
//                executable = "cmd";
//                // Windows XP/2000/NT/2003
//            }
//            arguments = new String[] {"/c", "set"};
//        } else if (OS.isFamilyZOS() || OS.isFamilyUnix()) {
//            // On most systems one could use: /bin/sh -c env
//
//            // Some systems have /bin/env, others /usr/bin/env, just try
//            if (new File("/bin/env").canRead()) {
//                executable = "/bin/env";
//            } else if (new File("/usr/bin/env").canRead()) {
//                executable = "/usr/bin/env";
//            } else {
//                // rely on PATH
//                executable = "env";
//            }
//        } else if (OS.isFamilyNetware() || OS.isFamilyOS400()) {
//            // rely on PATH
//            executable = "env";
//        } else {
//            // macOS 9 and previous
//            // TODO: I have no idea how to get it, someone must fix it
//            executable = null;
//        }
        final CommandLine commandLine = null;
//        if (executable != null) {
//            commandLine = new CommandLine(executable);
//            commandLine.addArguments(arguments);
//        }
        return commandLine;
    }

    /**
     * Gets the list of environment variables for this process.
     *
     * @return a map containing the environment variables.
     * @throws IOException obtaining the environment variables failed.
     */
    public synchronized Map<String, String> getProcEnvironment() throws IOException {
        if (procEnvironment == null) {
            procEnvironment = this.createProcEnvironment();
        }
        // create a copy of the map just in case that
        // anyone is going to modifiy it, e.g. removing
        // or setting an evironment variable
        final Map<String, String> copy = createEnvironmentMap();
        copy.putAll(procEnvironment);
        return copy;
    }

    /**
     * Runs a process to list the environment variables.
     *
     * @return a reader containing the output of the process.
     * @throws IOException starting the process failed.
     * @deprecated No longer needed.
     */
    @Deprecated
    protected BufferedReader runProcEnvCommand() throws IOException {
//        final ByteArrayOutputStream out = new ByteArrayOutputStream();
//        final Executor exe = DefaultExecutor.builder().get();
//        exe.setStreamHandler(new PumpStreamHandler(out));
//        // ignore the exit value - Just try to use what we got
//        exe.execute(getProcEnvCommand());
//        return new BufferedReader(new StringReader(toString(out)));
        return null;
    }

}
