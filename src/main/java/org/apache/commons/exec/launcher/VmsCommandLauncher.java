package org.apache.commons.exec.launcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.CommandLineImpl;
import org.apache.commons.exec.environment.Environment;

/**
 * A command launcher for VMS that writes the command to a temporary DCL script
 * before launching commands. This is due to limitations of both the DCL
 * interpreter and the Java VM implementation.
 */
public class VmsCommandLauncher extends Java13CommandLauncher {

    public VmsCommandLauncher() throws NoSuchMethodException {
        super();
    }

    /**
     * Launches the given command in a new process.
     */
    public Process exec(final CommandLine cmd, final Environment env)
            throws IOException {
        CommandLine vmsCmd = new CommandLineImpl();
        vmsCmd.setExecutable(createCommandFile(cmd, env).getPath());

        return super.exec(vmsCmd, env);
    }

    /**
     * Launches the given command in a new process, in the given working
     * directory. Note that under Java 1.3.1, 1.4.0 and 1.4.1 on VMS this method
     * only works if <code>workingDir</code> is null or the logical
     * JAVA$FORK_SUPPORT_CHDIR needs to be set to TRUE.
     */
    public Process exec(final CommandLine cmd, final Environment env,
            final File workingDir) throws IOException {
        CommandLine vmsCmd = new CommandLineImpl();
        vmsCmd.setExecutable(createCommandFile(cmd, env).getPath());

        return super.exec(vmsCmd, env, workingDir);
    }

    /*
     * Writes the command into a temporary DCL script and returns the
     * corresponding File object. The script will be deleted on exit.
     */
    private File createCommandFile(final CommandLine cmd, final Environment env)
            throws IOException {
        File script = File.createTempFile("ANT", ".COM");
        script.deleteOnExit();
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(script));

            // add the environment as logicals to the DCL script
            if (env != null) {
                Set entries = env.entrySet();

                for (Iterator iter = entries.iterator(); iter.hasNext();) {
                    Entry entry = (Entry) iter.next();
                    out.print("$ DEFINE/NOLOG ");
                    out.print(entry.getKey());
                    out.print(" \"");
                    out.print(entry.getValue());
                    out.println('\"');
                }
            }

            out.print("$ " + cmd.getExecutable());
            String[] args = cmd.getArguments();
            for (int i = 0; i < args.length; i++) {
                out.println(" -");
                out.print(args[i]);
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return script;
    }

}
