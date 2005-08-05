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
public class CommandLauncherImpl implements CommandLauncher {
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
    public Process exec(final CommandLine cmd, final Environment env,
            final File workingDir) throws IOException {
        if (workingDir == null) {
            return exec(cmd, env);
        }
        throw new IOException("Cannot execute a process in different "
                + "directory under this JVM");
    }
}
