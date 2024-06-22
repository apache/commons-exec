/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.exec;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import org.apache.commons.exec.launcher.CommandLauncher;
import org.apache.commons.exec.launcher.CommandLauncherFactory;

/**
 * The default class to start a subprocess. The implementation allows to
 * <ul>
 * <li>set a current working directory for the subprocess</li>
 * <li>provide a set of environment variables passed to the subprocess</li>
 * <li>capture the subprocess output of stdout and stderr using an ExecuteStreamHandler</li>
 * <li>kill long-running processes using an ExecuteWatchdog</li>
 * <li>define a set of expected exit values</li>
 * <li>terminate any started processes when the main process is terminating using a ProcessDestroyer</li>
 * </ul>
 *
 * The following example shows the basic usage:
 *
 * <pre>
 * Executor exec = DefaultExecutor.builder().get();
 * CommandLine cl = new CommandLine("ls -l");
 * int exitvalue = exec.execute(cl);
 * </pre>
 */
public class DefaultExecutor implements Executor {

    /**
     * Constructs a new builder.
     *
     * @param <T> The builder type.
     * @since 1.4.0
     */
    public static class Builder<T extends Builder<T>> implements Supplier<DefaultExecutor> {

        private ThreadFactory threadFactory;
        private ExecuteStreamHandler executeStreamHandler;
        private File workingDirectory;

        @SuppressWarnings("unchecked")
        T asThis() {
            return (T) this;
        }

        /**
         * Creates a new configured DefaultExecutor.
         *
         * @return a new configured DefaultExecutor.
         */
        @Override
        public DefaultExecutor get() {
            return new DefaultExecutor(threadFactory, executeStreamHandler, workingDirectory);
        }

        ExecuteStreamHandler getExecuteStreamHandler() {
            return executeStreamHandler;
        }

        ThreadFactory getThreadFactory() {
            return threadFactory;
        }

        File getWorkingDirectory() {
            return workingDirectory;
        }

        /**
         * Sets the PumpStreamHandler.
         *
         * @param executeStreamHandler the ExecuteStreamHandler, null resets to the default.
         * @return {@code this} instance.
         */
        public T setExecuteStreamHandler(final ExecuteStreamHandler executeStreamHandler) {
            this.executeStreamHandler = executeStreamHandler;
            return asThis();
        }

        /**
         * Sets the ThreadFactory.
         *
         * @param threadFactory the ThreadFactory, null resets to the default.
         * @return {@code this} instance.
         */
        public T setThreadFactory(final ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return asThis();
        }

        /**
         * Sets the working directory.
         *
         * @param workingDirectory the working directory., null resets to the default.
         * @return {@code this} instance.
         */
        public T setWorkingDirectory(final File workingDirectory) {
            this.workingDirectory = workingDirectory;
            return asThis();
        }

    }

    /**
     * Creates a new builder.
     *
     * @return a new builder.
     * @since 1.4.0
     */
    public static Builder<?> builder() {
        return new Builder<>();
    }

    /** Taking care of output and error stream. */
    private ExecuteStreamHandler executeStreamHandler;

    /** The working directory of the process. */
    private File workingDirectory;

    /** Monitoring of long running processes. */
    private ExecuteWatchdog watchdog;

    /** The exit values considered to be successful. */
    private int[] exitValues;

    /** Launches the command in a new process. */
    private final CommandLauncher launcher;

    /** Optional cleanup of started processes. */
    private ProcessDestroyer processDestroyer;

    /** Worker thread for asynchronous execution. */
    private Thread executorThread;

    /** The first exception being caught to be thrown to the caller. */
    private IOException exceptionCaught;

    /**
     * The thread factory.
     */
    private final ThreadFactory threadFactory;

    /**
     * Constructs a default {@code PumpStreamHandler} and sets the working directory of the subprocess to the current working directory.
     *
     * The {@code PumpStreamHandler} pumps the output of the subprocess into our {@code System.out} and {@code System.err} to avoid into our {@code System.out}
     * and {@code System.err} to avoid a blocked or deadlocked subprocess (see {@link Process Process}).
     *
     * @deprecated Use {@link Builder#get()}.
     */
    @Deprecated
    public DefaultExecutor() {
        this(Executors.defaultThreadFactory(), new PumpStreamHandler(), new File("."));
    }

    DefaultExecutor(final ThreadFactory threadFactory, final ExecuteStreamHandler executeStreamHandler, final File workingDirectory) {
        this.threadFactory = threadFactory != null ? threadFactory : Executors.defaultThreadFactory();
        this.executeStreamHandler = executeStreamHandler != null ? executeStreamHandler : new PumpStreamHandler();
        this.workingDirectory = workingDirectory != null ? workingDirectory : new File(".");
        this.launcher = CommandLauncherFactory.createVMLauncher();
        this.exitValues = new int[0];
    }

    private void checkWorkingDirectory() throws IOException {
        checkWorkingDirectory(workingDirectory);
    }

    private void checkWorkingDirectory(final File directory) throws IOException {
        if (directory != null && !directory.exists()) {
            throw new IOException(directory + " doesn't exist.");
        }
    }

    /**
     * Closes the Closeable, remembering any exception.
     *
     * @param closeable the {@link Closeable} to close.
     */
    private void closeCatch(final Closeable closeable) {
        try {
            closeable.close();
        } catch (final IOException e) {
            setExceptionCaught(e);
        }
    }

    /**
     * Closes the streams belonging to the given Process.
     *
     * @param process the {@link Process}.
     */
    @SuppressWarnings("resource")
    private void closeProcessStreams(final Process process) {
        closeCatch(process.getInputStream());
        closeCatch(process.getOutputStream());
        closeCatch(process.getErrorStream());
    }

    /**
     * Creates a thread waiting for the result of an asynchronous execution.
     *
     * @param runnable the runnable passed to the thread.
     * @param name     the name of the thread.
     * @return the thread
     */
    protected Thread createThread(final Runnable runnable, final String name) {
        return ThreadUtil.newThread(threadFactory, runnable, name, false);
    }

    /**
     * @see org.apache.commons.exec.Executor#execute(CommandLine)
     */
    @Override
    public int execute(final CommandLine command) throws ExecuteException, IOException {
        return execute(command, (Map<String, String>) null);
    }

    /**
     * @see org.apache.commons.exec.Executor#execute(CommandLine, org.apache.commons.exec.ExecuteResultHandler)
     */
    @Override
    public void execute(final CommandLine command, final ExecuteResultHandler handler) throws ExecuteException, IOException {
        execute(command, null, handler);
    }

    /**
     * @see org.apache.commons.exec.Executor#execute(CommandLine, java.util.Map)
     */
    @Override
    public int execute(final CommandLine command, final Map<String, String> environment) throws ExecuteException, IOException {
        checkWorkingDirectory();
        return executeInternal(command, environment, workingDirectory, executeStreamHandler);
    }

    /**
     * @see org.apache.commons.exec.Executor#execute(CommandLine, java.util.Map, org.apache.commons.exec.ExecuteResultHandler)
     */
    @Override
    public void execute(final CommandLine command, final Map<String, String> environment, final ExecuteResultHandler handler)
            throws ExecuteException, IOException {
        checkWorkingDirectory();
        if (watchdog != null) {
            watchdog.setProcessNotStarted();
        }
        executorThread = createThread(() -> {
            int exitValue = INVALID_EXITVALUE;
            try {
                exitValue = executeInternal(command, environment, workingDirectory, executeStreamHandler);
                handler.onProcessComplete(exitValue);
            } catch (final ExecuteException e) {
                handler.onProcessFailed(e);
            } catch (final Exception e) {
                handler.onProcessFailed(new ExecuteException("Execution failed", exitValue, e));
            }
        }, "CommonsExecDefaultExecutor");
        getExecutorThread().start();
    }

    /**
     * Execute an internal process. If the executing thread is interrupted while waiting for the child process to return the child process will be killed.
     *
     * @param command          the command to execute.
     * @param environment      the execution environment.
     * @param workingDirectory the working directory.
     * @param streams          process the streams (in, out, err) of the process.
     * @return the exit code of the process.
     * @throws IOException executing the process failed.
     */
    private int executeInternal(final CommandLine command, final Map<String, String> environment, final File workingDirectory,
            final ExecuteStreamHandler streams) throws IOException {
        final Process process;
        exceptionCaught = null;
        try {
            process = launch(command, environment, workingDirectory);
        } catch (final IOException e) {
            if (watchdog != null) {
                watchdog.failedToStart(e);
            }
            throw e;
        }
        try {
            setStreams(streams, process);
        } catch (final IOException e) {
            process.destroy();
            if (watchdog != null) {
                watchdog.failedToStart(e);
            }
            throw e;
        }
        streams.start();
        try {
            // add the process to the list of those to destroy if the VM exits
            if (getProcessDestroyer() != null) {
                getProcessDestroyer().add(process);
            }
            // associate the watchdog with the newly created process
            if (watchdog != null) {
                watchdog.start(process);
            }
            int exitValue = INVALID_EXITVALUE;
            try {
                exitValue = process.waitFor();
            } catch (final InterruptedException e) {
                process.destroy();
            } finally {
                // see https://bugs.sun.com/view_bug.do?bug_id=6420270
                // see https://issues.apache.org/jira/browse/EXEC-46
                // Process.waitFor should clear interrupt status when throwing InterruptedException
                // but we have to do that manually
                Thread.interrupted();
            }
            if (watchdog != null) {
                watchdog.stop();
            }
            try {
                streams.stop();
            } catch (final IOException e) {
                setExceptionCaught(e);
            }
            closeProcessStreams(process);
            if (getExceptionCaught() != null) {
                throw getExceptionCaught();
            }
            if (watchdog != null) {
                try {
                    watchdog.checkException();
                } catch (final IOException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new IOException(e);
                }
            }
            if (isFailure(exitValue)) {
                throw new ExecuteException("Process exited with an error: " + exitValue, exitValue);
            }
            return exitValue;
        } finally {
            // remove the process to the list of those to destroy if the VM exits
            if (getProcessDestroyer() != null) {
                getProcessDestroyer().remove(process);
            }
        }
    }

    /**
     * Gets the first IOException being thrown.
     *
     * @return the first IOException being caught.
     */
    private IOException getExceptionCaught() {
        return exceptionCaught;
    }

    /**
     * Gets the worker thread being used for asynchronous execution.
     *
     * @return the worker thread.
     */
    protected Thread getExecutorThread() {
        return executorThread;
    }

    /**
     * @see org.apache.commons.exec.Executor#getProcessDestroyer()
     */
    @Override
    public ProcessDestroyer getProcessDestroyer() {
        return processDestroyer;
    }

    /**
     * @see org.apache.commons.exec.Executor#getStreamHandler()
     */
    @Override
    public ExecuteStreamHandler getStreamHandler() {
        return executeStreamHandler;
    }

    /**
     * Gets the thread factory. Z
     *
     * @return the thread factory.
     */
    ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * @see org.apache.commons.exec.Executor#getWatchdog()
     */
    @Override
    public ExecuteWatchdog getWatchdog() {
        return watchdog;
    }

    /**
     * @see org.apache.commons.exec.Executor#getWorkingDirectory()
     */
    @Override
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    /** @see org.apache.commons.exec.Executor#isFailure(int) */
    @Override
    public boolean isFailure(final int exitValue) {
        if (exitValues == null) {
            return false;
        }
        if (exitValues.length == 0) {
            return launcher.isFailure(exitValue);
        }
        for (final int exitValue2 : exitValues) {
            if (exitValue2 == exitValue) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a process that runs a command.
     *
     * @param command          the command to run.
     * @param env              the environment for the command.
     * @param workingDirectory the working directory for the command.
     * @return the process started.
     * @throws IOException forwarded from the particular launcher used.
     */
    protected Process launch(final CommandLine command, final Map<String, String> env, final File workingDirectory) throws IOException {
        if (launcher == null) {
            throw new IllegalStateException("CommandLauncher can not be null");
        }
        checkWorkingDirectory(workingDirectory);
        return launcher.exec(command, env, workingDirectory);
    }

    /**
     * Sets the first IOException thrown.
     *
     * @param e the IOException.
     */
    private void setExceptionCaught(final IOException e) {
        if (exceptionCaught == null) {
            exceptionCaught = e;
        }
    }

    /** @see org.apache.commons.exec.Executor#setExitValue(int) */
    @Override
    public void setExitValue(final int value) {
        setExitValues(new int[] { value });
    }

    /** @see org.apache.commons.exec.Executor#setExitValues(int[]) */
    @Override
    public void setExitValues(final int[] values) {
        exitValues = values == null ? null : (int[]) values.clone();
    }

    /**
     * @see org.apache.commons.exec.Executor#setProcessDestroyer(ProcessDestroyer)
     */
    @Override
    public void setProcessDestroyer(final ProcessDestroyer processDestroyer) {
        this.processDestroyer = processDestroyer;
    }

    /**
     * @see org.apache.commons.exec.Executor#setStreamHandler(org.apache.commons.exec.ExecuteStreamHandler)
     */
    @Override
    public void setStreamHandler(final ExecuteStreamHandler streamHandler) {
        this.executeStreamHandler = streamHandler;
    }

    @SuppressWarnings("resource")
    private void setStreams(final ExecuteStreamHandler streams, final Process process) throws IOException {
        streams.setProcessInputStream(process.getOutputStream());
        streams.setProcessOutputStream(process.getInputStream());
        streams.setProcessErrorStream(process.getErrorStream());
    }

    /**
     * @see org.apache.commons.exec.Executor#setWatchdog(org.apache.commons.exec.ExecuteWatchdog)
     */
    @Override
    public void setWatchdog(final ExecuteWatchdog watchdog) {
        this.watchdog = watchdog;
    }

    /**
     * Sets the working directory.
     *
     * @see org.apache.commons.exec.Executor#setWorkingDirectory(java.io.File)
     * @deprecated Use {@link Builder#setWorkingDirectory(File)}.
     */
    @Deprecated
    @Override
    public void setWorkingDirectory(final File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

}
