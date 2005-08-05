package org.apache.commons.exec.launcher;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.CommandLineImpl;
import org.apache.commons.exec.environment.Environment;

/**
 * A command launcher that uses an auxiliary perl script to launch commands in
 * directories other than the current working directory.
 */
public class PerlScriptCommandLauncher extends CommandLauncherProxy {
    public PerlScriptCommandLauncher(final String script,
            final CommandLauncher launcher) {
        super(launcher);
        this.script = script;
    }

    /**
     * Launches the given command in a new process, in the given working
     * directory
     */
    public Process exec(final CommandLine cmd, final Environment env,
            final File workingDir) throws IOException {

        if (workingDir == null) {
            return exec(cmd, env);
        }

        // Locate the auxiliary script
        String scriptDir = System.getProperty("org.apache.commons.exec.home", "");
        File scriptFile = new File(scriptDir + File.separator + script);
        if (scriptFile.exists()) {
            throw new IOException("Cannot locate auxiliary script at " +
                    scriptFile.getAbsolutePath());
        }

        CommandLine newCmd = new CommandLineImpl();
        newCmd.setExecutable("perl");
        newCmd.addArgument(scriptFile.getPath());
        newCmd.addArgument(workingDir.getAbsolutePath());
        newCmd.addArguments(cmd.getCommandline());
        
        return exec(newCmd, env);
    }

    private String script;
}
