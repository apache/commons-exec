package org.apache.commons.exec.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A command launcher for JDK/JRE 1.3 (and higher). Uses the built-in
 * Runtime.exec() command
 */
public class Java13CommandLauncher extends CommandLauncherImpl {
    private static Log log = LogFactory.getLog(Java13CommandLauncher.class);

    public Java13CommandLauncher() throws NoSuchMethodException {
        // Locate method Runtime.exec(String[] cmdarray,
        // String[] envp, File dir)
        myExecWithCWD = Runtime.class.getMethod("exec", new Class[] {
                String[].class, String[].class, File.class});
    }

    /**
     * Launches the given command in a new process, in the given working
     * directory
     * 
     * @param project
     *            the ant project
     * @param cmd
     *            the command line to execute as an array of strings
     * @param env
     *            the environment to set as an array of strings
     * @param workingDir
     *            the working directory where the command should run
     * @throws IOException
     *             probably forwarded from Runtime#exec
     */
    public Process exec(final String[] cmd, final String[] env,
            final File workingDir) throws IOException {
        try {
            log.debug("Execute:Java13CommandLauncher: " + cmd);

            Object[] arguments = {cmd, env, workingDir};
            return (Process) myExecWithCWD.invoke(Runtime.getRuntime(),
                    arguments);
        } catch (InvocationTargetException exc) {
            Throwable realexc = exc.getTargetException();
            if (realexc instanceof ThreadDeath) {
                throw (ThreadDeath) realexc;
            } else if (realexc instanceof IOException) {
                throw (IOException) realexc;
            } else {
                throw new ExecuteException("Unable to execute command",
                        realexc);
            }
        } catch (Exception exc) {
            // IllegalAccess, IllegalArgument, ClassCast
            throw new ExecuteException("Unable to execute command", exc);
        }
    }

    private Method myExecWithCWD;
}
