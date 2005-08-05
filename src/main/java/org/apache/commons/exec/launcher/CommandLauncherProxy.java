package org.apache.commons.exec.launcher;

import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.environment.Environment;

/**
 * A command launcher that proxies another command launcher. Sub-classes
 * override exec(args, env, workdir)
 */
public abstract class CommandLauncherProxy extends CommandLauncherImpl {

    public CommandLauncherProxy(final CommandLauncher launcher) {
        myLauncher = launcher;
    }

    private CommandLauncher myLauncher;

    /**
     * Launches the given command in a new process. Delegates this method to the
     * proxied launcher
     * 
     * @param project
     *            the ant project
     * @param cmd
     *            the command line to execute as an array of strings
     * @param env
     *            the environment to set as an array of strings
     * @throws IOException
     *             forwarded from the exec method of the command launcher
     */
    public Process exec(final CommandLine cmd, final Environment env)
            throws IOException {
        return myLauncher.exec(cmd, env);
    }

}
