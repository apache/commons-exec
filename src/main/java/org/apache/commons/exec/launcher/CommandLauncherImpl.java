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
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.environment.Environment;

/**
 * A command launcher for a particular JVM/OS platform. This class is a general
 * purpose command launcher which can only launch commands in the current
 * working directory.
 */
public abstract class CommandLauncherImpl implements CommandLauncher {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.exec.launcher.CommandLauncherIn#exec(java.lang.String[],
     *      java.lang.String[])
     */
    public Process exec(final CommandLine cmd, final Environment env)
            throws IOException {
        String[] envVar = null;
        if(env != null) {
            envVar = env.getVariables();
        }
        
        return Runtime.getRuntime().exec(cmd.getCommandline(),
                envVar);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.exec.launcher.CommandLauncherIn#exec(java.lang.String[],
     *      java.lang.String[], java.io.File)
     */
    public abstract Process exec(final CommandLine cmd, final Environment env,
            final File workingDir) throws IOException;
}
