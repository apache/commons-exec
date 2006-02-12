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

import org.apache.commons.exec.environment.EnvironmentUtil;

/**
 * A command launcher for a particular JVM/OS platform. This class is a general
 * purpose command launcher which can only launch commands in the current
 * working directory.
 */
public abstract class CommandLauncherImpl implements CommandLauncher {

    public Process exec(final String[] cmd, final Map env)
            throws IOException {
        String[] envVar = null;
        if(env != null) {
            envVar = EnvironmentUtil.toStrings(env);
        }
        
        return Runtime.getRuntime().exec(cmd,
                envVar);
    }

    public abstract Process exec(final String[] cmd, final Map env,
            final File workingDir) throws IOException;
}
