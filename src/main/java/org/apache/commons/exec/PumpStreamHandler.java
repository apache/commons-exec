/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.commons.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.exec.util.DebugUtils;

/**
 * Copies standard output and error of sub-processes to standard output and error of the parent process. If output or error stream are set to null, any feedback
 * from that stream will be lost.
 */
public class PumpStreamHandler implements ExecuteStreamHandler {

    private static final Duration STOP_TIMEOUT_ADDITION = Duration.ofSeconds(2);

    private Thread outputThread;

    private Thread errorThread;

    private Thread inputThread;

    private final OutputStream outputStream;

    private final OutputStream errorOutputStream;

    private final InputStream inputStream;

    private InputStreamPumper inputStreamPumper;

    /** The timeout Duration the implementation waits when stopping the pumper threads. */
    private Duration stopTimeout = Duration.ZERO;

    /** The last exception being caught. */
    private IOException caught;

    /**
     * The thread factory.
     */
    private final ThreadFactory threadFactory;

    /**
     * Constructs a new {@link PumpStreamHandler}.
     */
    public PumpStreamHandler() {
        this(System.out, System.err);
    }

    /**
     * Constructs a new {@link PumpStreamHandler}.
     *
     * @param allOutputStream the output/error {@link OutputStream}. The {@code OutputStream}
     *      implementation must be thread-safe because the output and error reader threads will
     *      concurrently write to it.
     */
    public PumpStreamHandler(final OutputStream allOutputStream) {
        this(allOutputStream, allOutputStream);
    }

    /**
     * Constructs a new {@link PumpStreamHandler}.
     *
     * <p>If the same {@link OutputStream} instance is used for output and error, then it must be
     * thread-safe because the output and error reader threads will concurrently write to it.
     *
     * @param outputStream      the output {@link OutputStream}.
     * @param errorOutputStream the error {@link OutputStream}.
     */
    public PumpStreamHandler(final OutputStream outputStream, final OutputStream errorOutputStream) {
        this(outputStream, errorOutputStream, null);
    }

    /**
     * Constructs a new {@link PumpStreamHandler}.
     *
     * <p>If the same {@link OutputStream} instance is used for output and error, then it must be
     * thread-safe because the output and error reader threads will concurrently write to it.
     *
     * @param outputStream      the output {@link OutputStream}.
     * @param errorOutputStream the error {@link OutputStream}.
     * @param inputStream       the input {@link InputStream}.
     */
    public PumpStreamHandler(final OutputStream outputStream, final OutputStream errorOutputStream, final InputStream inputStream) {
        this(Executors.defaultThreadFactory(), outputStream, errorOutputStream, inputStream);
    }

    /**
     * Constructs a new {@link PumpStreamHandler}.
     *
     * <p>If the same {@link OutputStream} instance is used for output and error, then it must be
     * thread-safe because the output and error reader threads will concurrently write to it.
     *
     * @param outputStream      the output {@link OutputStream}.
     * @param errorOutputStream the error {@link OutputStream}.
     * @param inputStream       the input {@link InputStream}.
     */
    private PumpStreamHandler(final ThreadFactory threadFactory, final OutputStream outputStream, final OutputStream errorOutputStream,
            final InputStream inputStream) {
        this.threadFactory = threadFactory;
        this.outputStream = outputStream;
        this.errorOutputStream = errorOutputStream;
        this.inputStream = inputStream;
    }

    /**
     * Create the pump to handle error output.
     *
     * @param is the {@link InputStream}.
     * @param os the {@link OutputStream}.
     */
    protected void createProcessErrorPump(final InputStream is, final OutputStream os) {
        errorThread = createPump(is, os);
    }

    /**
     * Create the pump to handle process output.
     *
     * @param is the {@link InputStream}.
     * @param os the {@link OutputStream}.
     */
    protected void createProcessOutputPump(final InputStream is, final OutputStream os) {
        outputThread = createPump(is, os);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the given output stream. When the 'os' is an PipedOutputStream we are closing 'os' afterwards
     * to avoid an IOException ("Write end dead").
     *
     * @param is the input stream to copy from.
     * @param os the output stream to copy into.
     * @return the stream pumper thread.
     */
    protected Thread createPump(final InputStream is, final OutputStream os) {
        return createPump(is, os, os instanceof PipedOutputStream);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the given output stream.
     *
     * @param is                 the input stream to copy from.
     * @param os                 the output stream to copy into.
     * @param closeWhenExhausted close the output stream when the input stream is exhausted.
     * @return the stream pumper thread.
     */
    protected Thread createPump(final InputStream is, final OutputStream os, final boolean closeWhenExhausted) {
        return ThreadUtil.newThread(threadFactory, new StreamPumper(is, os, closeWhenExhausted), "CommonsExecStreamPumper-", true);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the given output stream.
     *
     * @param is the System.in input stream to copy from.
     * @param os the output stream to copy into.
     * @return the stream pumper thread.
     */
    private Thread createSystemInPump(final InputStream is, final OutputStream os) {
        inputStreamPumper = new InputStreamPumper(is, os);
        return ThreadUtil.newThread(threadFactory, inputStreamPumper, "CommonsExecStreamPumper-", true);
    }

    /**
     * Gets the error stream.
     *
     * @return {@link OutputStream}.
     */
    protected OutputStream getErr() {
        return errorOutputStream;
    }

    /**
     * Gets the output stream.
     *
     * @return {@link OutputStream}.
     */
    protected OutputStream getOut() {
        return outputStream;
    }

    Duration getStopTimeout() {
        return stopTimeout;
    }

    /**
     * Sets the {@link InputStream} from which to read the standard error of the process.
     *
     * @param is the {@link InputStream}.
     */
    @Override
    public void setProcessErrorStream(final InputStream is) {
        if (errorOutputStream != null) {
            createProcessErrorPump(is, errorOutputStream);
        }
    }

    /**
     * Sets the {@link OutputStream} by means of which input can be sent to the process.
     *
     * @param os the {@link OutputStream}.
     */
    @Override
    public void setProcessInputStream(final OutputStream os) {
        if (inputStream != null) {
            if (inputStream == System.in) {
                inputThread = createSystemInPump(inputStream, os);
            } else {
                inputThread = createPump(inputStream, os, true);
            }
        } else {
            try {
                os.close();
            } catch (final IOException e) {
                final String msg = "Got exception while closing output stream";
                DebugUtils.handleException(msg, e);
            }
        }
    }

    /**
     * Sets the {@link InputStream} from which to read the standard output of the process.
     *
     * @param is the {@link InputStream}.
     */
    @Override
    public void setProcessOutputStream(final InputStream is) {
        if (outputStream != null) {
            createProcessOutputPump(is, outputStream);
        }
    }

    /**
     * Sets maximum time to wait until output streams are exhausted when {@link #stop()} was called.
     *
     * @param timeout timeout or zero to wait forever (default).
     * @since 1.4.0
     */
    public void setStopTimeout(final Duration timeout) {
        this.stopTimeout = timeout != null ? timeout : Duration.ZERO;
    }

    /**
     * Sets maximum time to wait until output streams are exhausted when {@link #stop()} was called.
     *
     * @param timeout timeout in milliseconds or zero to wait forever (default).
     * @deprecated Use {@link #setStopTimeout(Duration)}.
     */
    @Deprecated
    public void setStopTimeout(final long timeout) {
        this.stopTimeout = Duration.ofMillis(timeout);
    }

    /**
     * Starts the {@link Thread}s.
     */
    @Override
    public void start() {
        start(outputThread);
        start(errorThread);
        start(inputThread);
    }

    /**
     * Starts the given {@link Thread}.
     */
    private void start(final Thread thread) {
        if (thread != null) {
            thread.start();
        }
    }

    /**
     * Stops pumping the streams. When a timeout is specified it is not guaranteed that the pumper threads are cleanly terminated.
     */
    @Override
    public void stop() throws IOException {
        if (inputStreamPumper != null) {
            inputStreamPumper.stopProcessing();
        }
        stop(outputThread, stopTimeout);
        stop(errorThread, stopTimeout);
        stop(inputThread, stopTimeout);

        if (errorOutputStream != null && errorOutputStream != outputStream) {
            try {
                errorOutputStream.flush();
            } catch (final IOException e) {
                final String msg = "Got exception while flushing the error stream : " + e.getMessage();
                DebugUtils.handleException(msg, e);
            }
        }

        if (outputStream != null) {
            try {
                outputStream.flush();
            } catch (final IOException e) {
                final String msg = "Got exception while flushing the output stream";
                DebugUtils.handleException(msg, e);
            }
        }

        if (caught != null) {
            throw caught;
        }
    }

    /**
     * Stops a pumper thread. The implementation actually waits longer than specified in 'timeout' to detect if the timeout was indeed exceeded. If the timeout
     * was exceeded an IOException is created to be thrown to the caller.
     *
     * @param thread  the thread to be stopped.
     * @param timeout the time in ms to wait to join.
     */
    private void stop(final Thread thread, final Duration timeout) {
        if (thread != null) {
            try {
                if (timeout.equals(Duration.ZERO)) {
                    thread.join();
                } else {
                    final Duration timeToWait = timeout.plus(STOP_TIMEOUT_ADDITION);
                    final Instant startTime = Instant.now();
                    thread.join(timeToWait.toMillis());
                    if (Instant.now().isAfter(startTime.plus(timeToWait))) {
                        caught = new ExecuteException("The stop timeout of " + timeout + " ms was exceeded", Executor.INVALID_EXITVALUE);
                    }
                }
            } catch (final InterruptedException e) {
                thread.interrupt();
            }
        }
    }

    /**
     * Stops a pumper thread. The implementation actually waits longer than specified in 'timeout' to detect if the timeout was indeed exceeded. If the timeout
     * was exceeded an IOException is created to be thrown to the caller.
     *
     * @param thread        the thread to be stopped.
     * @param timeoutMillis the time in ms to wait to join.
     */
    protected void stopThread(final Thread thread, final long timeoutMillis) {
        stop(thread, Duration.ofMillis(timeoutMillis));
    }
}
