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
import java.util.Map;

public interface CommandLauncher {

    /**
     * Launches the given command in a new process.
     * 
     * @param cmd
     *            The command to execute
     * @param env
     *            The environment for the new process. If null, the environment
     *            of the current process is used.
     * @throws IOException
     *             if attempting to run a command in a specific directory
     */
    Process exec(final String[] cmd, final Map env)
            throws IOException;

    /**
     * Launches the given command in a new process, in the given working
     * directory.
     * 
     * @param cmd
     *            The command to execute
     * @param env
     *            The environment for the new process. If null, the environment
     *            of the current process is used.
     * @param workingDir
     *            The directory to start the command in. If null, the current
     *            directory is used
     * @throws IOException
     *             if trying to change directory
     */
    Process exec(final String[] cmd, final Map env,
            final File workingDir) throws IOException;
}
