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
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.CommandLineImpl;

public class OpenVmsEnvironment extends Environment {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3762535598665117752L;

    protected OpenVmsEnvironment() {

    }

    public static synchronized Environment getProcEnvironment() {
        Environment procEnvironment = getProcEnvironment();

        if (procEnvironment == null) {
            procEnvironment = new Environment();
            try {
                BufferedReader in = runProcEnvCommand();

                procEnvironment = addVMSLogicals(procEnvironment, in);
                return procEnvironment;
            } catch (java.io.IOException exc) {
                exc.printStackTrace();
                // Just try to see how much we got
            }
        }

        return procEnvironment;
    }

    protected static CommandLine getProcEnvCommand() {
        CommandLine commandLine = new CommandLineImpl();
        commandLine.setExecutable("show");
        commandLine.addArgument("logical");
        return commandLine;
    }

    /**
     * This method is VMS specific and used by getProcEnvironment(). Parses VMS
     * logicals from <code>in</code> and adds them to <code>environment</code>.
     * <code>in</code> is expected to be the output of "SHOW LOGICAL". The
     * method takes care of parsing the output correctly as well as making sure
     * that a logical defined in multiple tables only gets added from the
     * highest order table. Logicals with multiple equivalence names are mapped
     * to a variable with multiple values separated by a comma (,).
     */
    private static Environment addVMSLogicals(final Environment environment,
            final BufferedReader in) throws IOException {
        HashMap logicals = new HashMap();
        String logName = null, logValue = null, newLogName;
        String line = null;
        while ((line = in.readLine()) != null) {
            // parse the VMS logicals into required format ("VAR=VAL[,VAL2]")
            if (line.startsWith("\t=")) {
                // further equivalence name of previous logical
                if (logName != null) {
                    logValue += "," + line.substring(4, line.length() - 1);
                }
            } else if (line.startsWith("  \"")) {
                // new logical?
                if (logName != null) {
                    logicals.put(logName, logValue);
                }
                int eqIndex = line.indexOf('=');
                newLogName = line.substring(3, eqIndex - 2);
                if (logicals.containsKey(newLogName)) {
                    // already got this logical from a higher order table
                    logName = null;
                } else {
                    logName = newLogName;
                    logValue = line.substring(eqIndex + 3, line.length() - 1);
                }
            }
        }
        // Since we "look ahead" before adding, there's one last env var.
        if (logName != null) {
            logicals.put(logName, logValue);
        }

        for (Iterator i = logicals.keySet().iterator(); i.hasNext();) {
            String logical = (String) i.next();
            environment.addVariable(logical, (String) logicals.get(logical));
        }
        return environment;
    }

}
