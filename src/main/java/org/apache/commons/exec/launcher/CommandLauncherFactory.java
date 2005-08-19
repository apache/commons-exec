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

import org.apache.commons.exec.OS;

/**
 * Builds a command launcher for the OS and JVM we are running under.
 */

public final class CommandLauncherFactory {

    private CommandLauncherFactory() {

    }

    /**
     * 
     */
    public static CommandLauncher createVMLauncher() {
        // Try using a JDK 1.3 launcher
        CommandLauncher launcher = null;
        try {
            if (OS.isFamilyOpenVms()) {
                launcher = new VmsCommandLauncher();
                // TODO why not use Java13CommandLauncher on OS2?
                //} else if (!OS.isFamilyOS2()) {
            } else {
                launcher = new Java13CommandLauncher();
            }
        } catch (NoSuchMethodException exc) {
            // Ignore and keep trying
        }

        return launcher;
    }
}
