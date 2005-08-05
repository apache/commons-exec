package org.apache.commons.exec.launcher;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.CommandLineImpl;
import org.apache.commons.exec.environment.Environment;

/**
 * A command launcher for Windows XP/2000/NT that uses 'cmd.exe' when launching
 * commands in directories other than the current working directory.
 */
public class WinNTCommandLauncher extends CommandLauncherProxy {
    public WinNTCommandLauncher(final CommandLauncher launcher) {
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

        // Use cmd.exe to change to the specified directory before running
        // the command
        CommandLine newCmd = new CommandLineImpl();
        newCmd.setExecutable("cmd");
        newCmd.addArgument("/c");
        newCmd.addArgument("cd");
        newCmd.addArgument("/d");
        newCmd.addArgument(workingDir.getAbsolutePath());
        newCmd.addArgument("&&");
        newCmd.addArguments(cmd.getCommandline());

        return exec(newCmd, env);
    }
}
