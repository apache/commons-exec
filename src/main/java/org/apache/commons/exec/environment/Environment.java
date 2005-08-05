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

package org.apache.commons.exec.environment;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.CommandLineImpl;
import org.apache.commons.exec.Execute;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;

/**
 * Wrapper for environment variables.
 */
public class Environment extends HashMap {

    public static Environment createEnvironment() {
        if (OS.isFamilyOpenVms()) {
            return new OpenVmsEnvironment();
        } else {
            return new Environment();
        }
    }

    public static Environment createEnvironment(String[] envVars) {
        Environment env = createEnvironment();

        if(envVars != null) {
            for (int i = 0; i < envVars.length; i++) {
                env.addVariable(EnvironmentVariable
                        .createEnvironmentVariable(envVars[i]));
            }
        }
        return env;
    }

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3256443594801165364L;

    /**
     * a vector of type EnvirommentVariable
     * 
     * @see EnvironmentVariable
     */

    private static Environment procEnvironment;

    /**
     * constructor
     */
    protected Environment() {
    }

    /**
     * add a variable. Validity checking is <i>not</i> performed at this point.
     * Duplicates are not caught either.
     * 
     * @param var
     *            new variable.
     */
    public void addVariable(final EnvironmentVariable var) {
        put(var.getKey(), var);
    }

    public void addVariable(final String key, final String value) {
        put(key, EnvironmentVariable.createEnvironmentVariable(key, value));
    }

    /**
     * get the variable list as an array
     * 
     * @return array of key=value assignment strings
     */
    public String[] getVariables() {
        if (size() == 0) {
            return null;
        }
        String[] result = new String[size()];
        int i = 0;
        for (Iterator iter = entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();

            result[i] = ((EnvironmentVariable) entry.getValue()).toString();
            i++;
        }
        return result;
    }

    /**
     * Find the list of environment variables for this process.
     * 
     * @return a vector containing the environment variables the vector elements
     *         are strings formatted like variable = value
     */
    public static synchronized Environment getProcEnvironment() {
        if (procEnvironment != null) {
            return procEnvironment;
        }

        procEnvironment = new Environment();
        try {
            BufferedReader in = runProcEnvCommand();

            String var = null;
            String line, lineSep = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                if (line.indexOf('=') == -1) {
                    // Chunk part of previous env var (UNIX env vars can
                    // contain embedded new lines).
                    if (var == null) {
                        var = lineSep + line;
                    } else {
                        var += lineSep + line;
                    }
                } else {
                    // New env var...append the previous one if we have it.
                    if (var != null) {
                        procEnvironment.addVariable(EnvironmentVariable
                                .createEnvironmentVariable(var));
                    }
                    var = line;
                }
            }
            // Since we "look ahead" before adding, there's one last env var.
            if (var != null) {
                procEnvironment.addVariable(EnvironmentVariable
                        .createEnvironmentVariable(var));
            }
        } catch (java.io.IOException exc) {
            exc.printStackTrace();
            // Just try to see how much we got
        }
        return procEnvironment;
    }

    /**
     * @return
     * @throws IOException
     */
    protected static BufferedReader runProcEnvCommand() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Execute exe = new Execute(new PumpStreamHandler(out));
        exe.setCommandline(getProcEnvCommand());
        // Make sure we do not recurse forever
        exe.setNewEnvironment(true);
        int retval = exe.execute();
        if (retval != 0) {
            // Just try to use what we got
        }
        BufferedReader in = new BufferedReader(new StringReader(toString(out)));
        return in;
    }

    protected static CommandLine getProcEnvCommand() {
        CommandLine commandLine = new CommandLineImpl();
        if (OS.isFamilyOS2()) {
            // OS/2 - use same mechanism as Windows 2000
            commandLine.setExecutable("cmd");
            commandLine.addArguments(new String[] {"/c", "set"});
        } else if (OS.isFamilyWindows()) {
            // Determine if we're running under XP/2000/NT or 98/95
            if (OS.isFamilyWin9x()) {
                commandLine.setExecutable("command.com");
                // Windows 98/95
            } else {
                commandLine.setExecutable("cmd");
                // Windows XP/2000/NT/2003
            }
            commandLine.addArguments(new String[] {"/c", "set"});
        } else if (OS.isFamilyZOS() || OS.isFamilyUnix()) {
            // On most systems one could use: /bin/sh -c env

            // Some systems have /bin/env, others /usr/bin/env, just try
            if (new File("/bin/env").canRead()) {
                commandLine.setExecutable("/bin/env");
            } else if (new File("/usr/bin/env").canRead()) {
                commandLine.setExecutable("/usr/bin/env");
            } else {
                // rely on PATH
                commandLine.setExecutable("env");
            }
        } else if (OS.isFamilyNetware() || OS.isFamilyOS400()) {
            // rely on PATH
            commandLine.setExecutable("env");
        } else {
            // MAC OS 9 and previous
            // TODO: I have no idea how to get it, someone must fix it
            commandLine = null;
        }
        return commandLine;
    }

    /**
     * ByteArrayOutputStream#toString doesn't seem to work reliably on OS/390,
     * at least not the way we use it in the execution context.
     * 
     * @param bos
     *            the output stream that one wants to read
     * @return the output stream as a string, read with special encodings in the
     *         case of z/os and os/400
     */
    private static String toString(final ByteArrayOutputStream bos) {
        if (OS.isFamilyZOS()) {
            try {
                return bos.toString("Cp1047");
            } catch (java.io.UnsupportedEncodingException e) {
                // noop default encoding used
            }
        } else if (OS.isFamilyOS400()) {
            try {
                return bos.toString("Cp500");
            } catch (java.io.UnsupportedEncodingException e) {
                // noop default encoding used
            }
        }
        return bos.toString();
    }

}
