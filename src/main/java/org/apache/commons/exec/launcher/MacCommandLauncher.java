package org.apache.commons.exec.launcher;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.environment.Environment;

/**
 * A command launcher for Mac that uses a dodgy mechanism to change working
 * directory before launching commands.
 */
public class MacCommandLauncher extends CommandLauncherProxy {
    public MacCommandLauncher(final CommandLauncher launcher) {
        super(launcher);
    }

    /**
     * Launches the given command in a new process, in the given working
     * directory.
     * 
     * @param cmd
     *            the command line to execute as an array of strings
     * @param env
     *            the environment to set as an array of strings
     * @param workingDir
     *            working directory where the command should run
     * @throws IOException
     *             forwarded from the exec method of the command launcher
     */
    public Process exec(final CommandLine cmd, final Environment env,
            final File workingDir) throws IOException {
        if (workingDir == null) {
            return exec(cmd, env);
        }

        String oldUserDir = System.getProperty("user.dir");

        System.getProperties().put("user.dir", workingDir.getAbsolutePath());
        try {
            return exec(cmd, env);
        } finally {
            System.getProperties().put("user.dir", oldUserDir);
        }
    }
}
