package org.apache.commons.exec.launcher;

import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.CommandLineImpl;
import org.apache.commons.exec.environment.Environment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A command launcher for JDK/JRE 1.1 under Windows. Fixes quoting problems in
 * Runtime.exec(). Can only launch commands in the current working directory
 */
public class Java11CommandLauncher extends CommandLauncherImpl {

    private static final Log LOG = LogFactory
            .getLog(Java11CommandLauncher.class);

    /**
     * Launches the given command in a new process. Needs to quote arguments
     * 
     * @param cmd
     *            the command line to execute as an array of strings
     * @param env
     *            the environment to set as an array of strings
     * @throws IOException
     *             probably forwarded from Runtime#exec
     */
    public Process exec(final CommandLine cmd, final Environment env)
            throws IOException {
        // Need to quote arguments with spaces, and to escape
        // quote characters
        CommandLine newCmd = new CommandLineImpl();
        newCmd.setExecutable(
                CommandLineImpl.quoteArgument(cmd.getExecutable()));

        String[] args = cmd.getArguments();
        for (int i = 0; i < args.length; i++) {
            newCmd.addArgument(CommandLineImpl.quoteArgument(args[i]));
        }
        LOG.debug("Execute:Java11CommandLauncher: " + newCmd);

        return Runtime.getRuntime().exec(newCmd.getCommandline(),
                env.getVariables());
    }
}
