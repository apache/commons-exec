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

package org.apache.commons.exec.launcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A command launcher for VMS that writes the command to a temporary DCL script
 * before launching commands. This is due to limitations of both the DCL
 * interpreter and the Java VM implementation.
 */
public class VmsCommandLauncher extends Java13CommandLauncher {

    /**
     * Launches the given command in a new process.
     */
    public Process exec(final String[] cmd, final Map env)
            throws IOException {
        String[] vmsCmd = new String[1];
        vmsCmd[0] = createCommandFile(cmd, env).getPath();

        return super.exec(vmsCmd, env);
    }

    /**
     * Launches the given command in a new process, in the given working
     * directory. Note that under Java 1.3.1, 1.4.0 and 1.4.1 on VMS this method
     * only works if <code>workingDir</code> is null or the logical
     * JAVA$FORK_SUPPORT_CHDIR needs to be set to TRUE.
     */
    public Process exec(final String[] cmd, final Map env,
            final File workingDir) throws IOException {
        String[] vmsCmd = new String[1];
        vmsCmd[0] = createCommandFile(cmd, env).getPath();

        return super.exec(vmsCmd, env, workingDir);
    }

    /*
     * Writes the command into a temporary DCL script and returns the
     * corresponding File object. The script will be deleted on exit.
     */
    private File createCommandFile(final String[] cmd, final Map env)
            throws IOException {
        File script = File.createTempFile("EXEC", ".COM");
        script.deleteOnExit();
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(script));

            // add the environment as logicals to the DCL script
            if (env != null) {
                Set entries = env.entrySet();

                for (Iterator iter = entries.iterator(); iter.hasNext();) {
                    Entry entry = (Entry) iter.next();
                    out.print("$ DEFINE/NOLOG ");
                    out.print(entry.getKey());
                    out.print(" \"");
                    out.print(entry.getValue());
                    out.println('\"');
                }
            }

            if(cmd.length == 0) {
            	throw new IOException("Can not execute empty command");
            } else {
	            out.print("$ " + cmd[0]);
	
	            for (int i = 1; i < cmd.length; i++) {
	                out.println(" -");
	                out.print(cmd[i]);
	            }
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return script;
    }
}
