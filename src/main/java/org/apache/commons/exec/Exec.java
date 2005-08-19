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

package org.apache.commons.exec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.exec.environment.Environment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Executes a given command if the os platform is appropriate.
 */
public class Exec {
    private static Log log = LogFactory.getLog(Exec.class);

    private File dir;

    private boolean newEnvironment = false;

    private Long timeout = null;

    private String executable;

    private boolean resolveExecutable = false;

    private boolean spawn = false;

    private boolean incompatibleWithSpawn = false;

    public Exec() {
        // default cnstr
    }

    /**
     * Set whether or not you want the process to be spawned default is not
     * spawned.
     * 
     * @param spawn
     *            if true you do not want to wait for the end of the process
     */
    public void setSpawn(final boolean spawn) {
        this.spawn = spawn;
    }

    /**
     * Timeout in milliseconds after which the process will be killed.
     * 
     * @param value
     *            timeout in milliseconds
     */
    public void setTimeout(final Long value) {
        timeout = value;
        incompatibleWithSpawn = true;
    }

    /**
     * Timeout in milliseconds after which the process will be killed.
     * 
     * @param value
     *            timeout in milliseconds
     */
    public void setTimeout(final Integer value) {
        if (value == null) {
            timeout = null;
        } else {
            setTimeout(new Long(value.intValue()));
        }
        incompatibleWithSpawn = true;
    }

    /**
     * Set the working directory of the process.
     * 
     * @param d
     *            the working directory of the process
     */
    public void setDir(final File d) {
        this.dir = d;
    }

    /**
     * Do not propagate old environment when new environment variables are
     * specified.
     * 
     * @param newenv
     *            if true, do not propagate old environment when new environment
     *            variables are specified.
     */
    public void setNewEnvironment(final boolean newenv) {
        newEnvironment = newenv;
    }

    /**
     * Sets a flag indicating whether to attempt to resolve the executable to a
     * file.
     * 
     * @param resolveExecutable
     *            if true, attempt to resolve the path of the executable
     */
    public void setResolveExecutable(final boolean resolveExecutable) {
        this.resolveExecutable = resolveExecutable;
    }

    /**
     * Indicates whether to attempt to resolve the executable to a file.
     */
    public boolean isResolveExecutable() {
        return resolveExecutable;
    }

    /**
     * The method attempts to figure out where the executable is so that we can
     * feed the full path. We first try basedir, then the exec dir, and then
     * fallback to the straight executable name (i.e. on thier path).
     * 
     * @param exec
     *            the name of the executable
     * @param searchPath
     *            if true, the excutable will be looked up in the PATH
     *            environment and the absolute path is returned.
     * @return the executable as a full path if it can be determined.
     */
    protected String resolveExecutable(final String exec,
            final boolean searchPath) {
        if (!resolveExecutable) {
            return exec;
        }

        // try to find the executable
        File executableFile = new File(exec);
        if (executableFile.exists()) {
            return executableFile.getAbsolutePath();
        }

        // FileUtils fileUtils = FileUtils.newFileUtils();
        // now try to resolve against the dir if given
        if (dir != null) {
            executableFile = new File(dir, exec);
            if (executableFile.exists()) {
                return executableFile.getAbsolutePath();
            }
        }

        // couldn't find it - must be on path
        if (searchPath) {
            // Vector env = Execute.getProcEnvironment();
            // Enumeration e = env.elements();

            // TODO should this be implemented?

            /*
             * Path p = null; while (e.hasMoreElements()) { String line =
             * (String) e.nextElement(); if (line.startsWith("PATH=") ||
             * line.startsWith("Path=")) { p = new Path(getProject(),
             * line.substring(5)); break; } }
             */

            /*
             * if (p != null) { String[] dirs = p.list(); for (int i = 0; i <
             * dirs.length; i++) { executableFile = new File(dirs[i], exec); if
             * (executableFile.exists()) { return
             * executableFile.getAbsolutePath(); } } }
             */
        }

        // searchPath is false, or no PATH or not found - keep our
        // fingers crossed.
        return exec;
    }

    /**
     * Do the work.
     * 
     * @throws IOException
     *             in a number of circumstances :
     *             <ul>
     *             <li>if failIfExecFails is set to true and the process cannot
     *             be started</li>
     *             <li>the java13command launcher can send build exceptions</li>
     *             <li>this list is not exhaustive or limitative</li>
     *             </ul>
     */
    public void execute(final CommandLine cl) throws IOException {
        execute(cl, null, new LogOutputStream(1), new LogOutputStream(2));
    }

    public void execute(final CommandLine cl, final Environment env)
            throws IOException {
        execute(cl, env, new LogOutputStream(1), new LogOutputStream(2));
    }

    public void execute(final CommandLine cl, final OutputStream out,
            final OutputStream error) throws IOException {
        execute(cl, null, out, error);

    }

    public void execute(final CommandLine cmdl, final Environment env,
            final OutputStream out, final OutputStream error)
            throws IOException {
        File savedDir = dir; // possibly altered in prepareExec

        Environment environment;
        if (env == null) {
            environment = Environment.createEnvironment();
        } else {
            environment = env;
        }

        cmdl.setExecutable(resolveExecutable(executable, false));
        checkConfiguration(cmdl);

        try {
            Execute exe = prepareExec(environment, out, error);
            runExec(exe, cmdl);
        } finally {
            dir = savedDir;
        }
    }

    public void execute(final CommandLine cmdl, final Environment env,
            final InputStream in, final OutputStream out,
            final OutputStream error) throws ExecuteException {
        File savedDir = dir; // possibly altered in prepareExec

        Environment environment;
        if (env == null) {
            environment = Environment.createEnvironment();
        } else {
            environment = env;
        }

        cmdl.setExecutable(resolveExecutable(executable, false));
        checkConfiguration(cmdl);

        try {
            Execute exe = prepareExec(environment, in, out, error);
            runExec(exe, cmdl);
        } finally {
            dir = savedDir;
        }
    }

    /**
     * Has the user set all necessary attributes?
     * 
     * @throws ExecuteException
     *             if there are missing required parameters
     */
    protected void checkConfiguration(final CommandLine cmdl)
            throws ExecuteException {
        if (cmdl.getExecutable() == null) {
            throw new ExecuteException("No executable specified");
        }
        if (dir != null && !dir.exists()) {
            throw new ExecuteException("The directory you specified does not "
                    + "exist");
        }
        if (dir != null && !dir.isDirectory()) {
            throw new ExecuteException("The directory you specified is not a "
                    + "directory");
        }
        if (spawn && incompatibleWithSpawn) {
            throw new ExecuteException("Spawn also does not allow timeout");
        }
        // setupRedirector();
    }

    /**
     * Set up properties on the redirector that we needed to store locally.
     */
    /*
     * protected void setupRedirector() { redirector.setInput(input);
     * redirector.setInputString(inputString); redirector.setOutput(output);
     * redirector.setError(error); }
     */

    /**
     * Create an Execute instance with the correct working directory set.
     * 
     * @param error
     * @param out
     * @return an instance of the Execute class
     * @throws ExecuteException
     *             under unknown circumstances.
     */
    protected Execute prepareExec(final Environment env,
            final OutputStream out, final OutputStream error)
            throws ExecuteException {
        return prepareExec(env, null, out, error);
    }

    protected Execute prepareExec(final Environment env, final InputStream in,
            final OutputStream out, final OutputStream error)
            throws ExecuteException {
        // default directory to the current directory
        /*if (dir == null) {
            // TODO does this work on all platforms?
            dir = new File(".");
        }*/

        Execute exe = new Execute(createHandler(in, out, error),
                createWatchdog());

        exe.setWorkingDirectory(dir);

        exe.setNewEnvironment(newEnvironment);
        exe.setEnvironment(env.getVariables());
        return exe;
    }

    /**
     * A Utility method for this classes and subclasses to run an Execute
     * instance (an external command).
     * 
     * @param exe
     *            instance of the execute class
     * @throws IOException
     *             in case of problem to attach to the stdin/stdout/stderr
     *             streams of the process
     */
    protected final void runExecute(final Execute exe) throws IOException {
        int returnCode = -1; // assume the worst

        if (!spawn) {
            returnCode = exe.execute();

            // test for and handle a forced process death
            if (exe.killedProcess()) {
                throw new ExecuteException("Timeout: killed the sub-process");
            }

            // redirector.complete();
            if (Execute.isFailure(returnCode)) {
                throw new ExecuteException(exe.getCommandline().getExecutable()
                        + " returned: " + returnCode);
            }
        } else {
            exe.spawn();
        }
    }

    /**
     * Run the command using the given Execute instance. This may be overridden
     * by subclasses
     * 
     * @param exe
     *            instance of Execute to run
     * @throws ExecuteException
     *             if the new process could not be started only if
     *             failIfExecFails is set to true (the default)
     */
    protected void runExec(final Execute exe, final CommandLine cmdl)
            throws ExecuteException {
        // show the command
        log.debug(cmdl.toString());

        exe.setCommandline(cmdl);
        try {
            runExecute(exe);
        } catch (IOException e) {
            throw new ExecuteException("Execute failed: " + e.toString(), e);
        } finally {
            // close the output file if required
            logFlush();
        }
    }

    /**
     * Create the StreamHandler to use with our Execute instance.
     * 
     * @param error
     * @param out
     * @return instance of ExecuteStreamHandler
     * @throws ExecuteException
     *             under unknown circumstances
     */
    protected ExecuteStreamHandler createHandler(final InputStream in,
            final OutputStream out, final OutputStream error)
            throws ExecuteException {
        return new PumpStreamHandler(out, error, in);
    }

    /**
     * Create the Watchdog to kill a runaway process.
     * 
     * @return instance of ExecuteWatchdog
     * @throws ExecuteException
     *             under unknown circumstances
     */
    protected ExecuteWatchdog createWatchdog() throws ExecuteException {
        if (timeout == null) {
            return null;
        }

        return new ExecuteWatchdog(timeout.longValue());
    }

    /**
     * Flush the output stream - if there is one.
     */
    protected void logFlush() {
    }

}
