package org.apache.commons.exec.launcher;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.environment.Environment;

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
    Process exec(final CommandLine cmd, final Environment env)
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
    Process exec(final CommandLine cmd, final Environment env,
            final File workingDir) throws IOException;

}