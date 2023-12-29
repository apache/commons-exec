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

import org.apache.commons.exec.util.DebugUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;

/**
 * Copies standard output and error of sub-processes to standard output and error of the parent process. If output or error stream are set to null, any feedback
 * from that stream will be lost.
 */
public class PumpStreamHandler implements ExecuteStreamHandler {

    private static final long STOP_TIMEOUT_ADDITION_MILLIS = 2000L;

    private Thread outputThread;

    private Thread errorThread;

    private Thread inputThread;

    private final OutputStream out;

    private final OutputStream err;

    private final InputStream input;

    private InputStreamPumper inputStreamPumper;

    /** The timeout in ms the implementation waits when stopping the pumper threads */
    private long stopTimeout;

    /** The last exception being caught */
    private IOException caught;

    /**
     * Constructs a new {@link PumpStreamHandler}.
     */
    public PumpStreamHandler() {
        this(System.out, System.err);
    }

    /**
     * Constructs a new {@link PumpStreamHandler}.
     *
     * @param outAndErr the output/error {@link OutputStream}.
     */
    public PumpStreamHandler(final OutputStream outAndErr) {
        this(outAndErr, outAndErr);
    }

    /**
     * Constructs a new {@link PumpStreamHandler}.
     *
     * @param out the output {@link OutputStream}.
     * @param err the error {@link OutputStream}.
     */
    public PumpStreamHandler(final OutputStream out, final OutputStream err) {
        this(out, err, null);
    }

    /**
     * Constructs a new {@link PumpStreamHandler}.
     *
     * @param out   the output {@link OutputStream}.
     * @param err   the error {@link OutputStream}.
     * @param input the input {@link InputStream}.
     */
    public PumpStreamHandler(final OutputStream out, final OutputStream err, final InputStream input) {
        this.out = out;
        this.err = err;
        this.input = input;
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
     * @param is the input stream to copy from
     * @param os the output stream to copy into
     * @return the stream pumper thread
     */
    protected Thread createPump(final InputStream is, final OutputStream os) {
        return createPump(is, os, os instanceof PipedOutputStream);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the given output stream.
     *
     * @param is                 the input stream to copy from
     * @param os                 the output stream to copy into
     * @param closeWhenExhausted close the output stream when the input stream is exhausted
     * @return the stream pumper thread
     */
    protected Thread createPump(final InputStream is, final OutputStream os, final boolean closeWhenExhausted) {
        final Thread result = new Thread(new StreamPumper(is, os, closeWhenExhausted), "Exec Stream Pumper");
        result.setDaemon(true);
        return result;
    }

    /**
     * Creates a stream pumper to copy the given input stream to the given output stream.
     *
     * @param is the System.in input stream to copy from
     * @param os the output stream to copy into
     * @return the stream pumper thread
     */
    private Thread createSystemInPump(final InputStream is, final OutputStream os) {
        inputStreamPumper = new InputStreamPumper(is, os);
        final Thread result = new Thread(inputStreamPumper, "Exec Input Stream Pumper");
        result.setDaemon(true);
        return result;
    }

    /**
     * Gets the error stream.
     *
     * @return {@link OutputStream}.
     */
    protected OutputStream getErr() {
        return err;
    }

    /**
     * Gets the output stream.
     *
     * @return {@link OutputStream}.
     */
    protected OutputStream getOut() {
        return out;
    }

    /**
     * Sets the {@link InputStream} from which to read the standard error of the process.
     *
     * @param is the {@link InputStream}.
     */
    @Override
    public void setProcessErrorStream(final InputStream is) {
        if (err != null) {
            createProcessErrorPump(is, err);
        }
    }

    /**
     * Sets the {@link OutputStream} by means of which input can be sent to the process.
     *
     * @param os the {@link OutputStream}.
     */
    @Override
    public void setProcessInputStream(final OutputStream os) {
        if (input != null) {
            if (input == System.in) {
                inputThread = createSystemInPump(input, os);
            } else {
                inputThread = createPump(input, os, true);
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
        if (out != null) {
            createProcessOutputPump(is, out);
        }
    }

    /**
     * Sets maximum time to wait until output streams are exchausted when {@link #stop()} was called.
     *
     * @param timeout timeout in milliseconds or zero to wait forever (default)
     */
    public void setStopTimeout(final long timeout) {
        this.stopTimeout = timeout;
    }

    /**
     * Start the {@link Thread}s.
     */
    @Override
    public void start() {
        if (outputThread != null) {
            outputThread.start();
        }
        if (errorThread != null) {
            errorThread.start();
        }
        if (inputThread != null) {
            inputThread.start();
        }
    }

    /**
     * Stop pumping the streams. When a timeout is specified it is not guaranteed that the pumper threads are cleanly terminated.
     */
    @Override
    public void stop() throws IOException {

        if (inputStreamPumper != null) {
            inputStreamPumper.stopProcessing();
        }

        stopThread(outputThread, stopTimeout);
        stopThread(errorThread, stopTimeout);
        stopThread(inputThread, stopTimeout);

        if (err != null && err != out) {
            try {
                err.flush();
            } catch (final IOException e) {
                final String msg = "Got exception while flushing the error stream : " + e.getMessage();
                DebugUtils.handleException(msg, e);
            }
        }

        if (out != null) {
            try {
                out.flush();
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
     * Stopping a pumper thread. The implementation actually waits longer than specified in 'timeout' to detect if the timeout was indeed exceeded. If the
     * timeout was exceeded an IOException is created to be thrown to the caller.
     *
     * @param thread        the thread to be stopped
     * @param timeoutMillis the time in ms to wait to join
     */
    protected void stopThread(final Thread thread, final long timeoutMillis) {

        if (thread != null) {
            try {
                if (timeoutMillis == 0) {
                    thread.join();
                } else {
                    final long timeToWaitMillis = timeoutMillis + STOP_TIMEOUT_ADDITION_MILLIS;
                    final long startTimeMillis = System.currentTimeMillis();
                    thread.join(timeToWaitMillis);
                    if (System.currentTimeMillis() > startTimeMillis + timeToWaitMillis) {
                        final String msg = "The stop timeout of " + timeoutMillis + " ms was exceeded";
                        caught = new ExecuteException(msg, Executor.INVALID_EXITVALUE);
                    }
                }
            } catch (final InterruptedException e) {
                thread.interrupt();
            }
        }
    }
}
