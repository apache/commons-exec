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
 *
 */

package org.apache.commons.exec;

import org.apache.commons.exec.util.DebugUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies standard output and error of subprocesses to standard output and error
 * of the parent process. If output or error stream are set to null, any feedback
 * from that stream will be lost. 
 */
public class PumpStreamHandler implements ExecuteStreamHandler {

    private Thread outputThread;

    private Thread errorThread;

    private Thread inputThread;

    private final OutputStream out;

    private final OutputStream err;

    private final InputStream input;

    private InputStreamPumper inputStreamPumper;
    
    private boolean alwaysWaitForStreamThreads = true;

    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     */
    public PumpStreamHandler() {
        this(System.out, System.err);
    }

    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     *
     * @param outAndErr
     *            the output/error <CODE>OutputStream</CODE>.
     */
    public PumpStreamHandler(final OutputStream outAndErr) {
        this(outAndErr, outAndErr);
    }
    
    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     *
     * @param out
     *            the output <CODE>OutputStream</CODE>.
     * @param err
     *            the error <CODE>OutputStream</CODE>.
     */
    public PumpStreamHandler(final OutputStream out, final OutputStream err) {
        this(out, err, null);
    }

    /**
     * Construct a new <CODE>PumpStreamHandler</CODE>.
     * 
     * @param out
     *            the output <CODE>OutputStream</CODE>.
     * @param err
     *            the error <CODE>OutputStream</CODE>.
     * @param input
     *            the input <CODE>InputStream</CODE>.
     */
    public PumpStreamHandler(final OutputStream out, final OutputStream err,
            final InputStream input) {

        this.out = out;
        this.err = err;
        this.input = input;
    }

    /**
     * Set the <CODE>InputStream</CODE> from which to read the standard output
     * of the process.
     * 
     * @param is
     *            the <CODE>InputStream</CODE>.
     */
    public void setProcessOutputStream(final InputStream is) {
        if (out != null) {
            createProcessOutputPump(is, out);
        }
    }

    /**
     * Set the <CODE>InputStream</CODE> from which to read the standard error
     * of the process.
     * 
     * @param is
     *            the <CODE>InputStream</CODE>.
     */
    public void setProcessErrorStream(final InputStream is) {
        if (err != null) {
            createProcessErrorPump(is, err);
        }
    }

    /**
     * Set the <CODE>OutputStream</CODE> by means of which input can be sent
     * to the process.
     * 
     * @param os
     *            the <CODE>OutputStream</CODE>.
     */
    public void setProcessInputStream(final OutputStream os) {
        if (input != null) {
            if (input == System.in) {
                inputThread = createSystemInPump(input, os);
        } else {
                inputThread = createPump(input, os, true);
            }        } 
        else {
            try {
                os.close();
            } catch (IOException e) {
                String msg = "Got exception while closing output stream";
                DebugUtils.handleException(msg ,e);
            }
        }
    }
    
    
    /**
     * Whether to always wait for (join) stream threads, even if the process
     * is was "killed" by a Watchdog.
     * @return true, to wait always (original behavior); false, to NOT wait if killed 
     */
    public boolean isAlwaysWaitForStreamThreads() {
        return alwaysWaitForStreamThreads;
    }

    /**
     * Whether to always wait for (join) stream threads, even if the process
     * is was "killed" by a Watchdog. Please note that skipping the wait might
     * leave up to three threads behind so and cause severe problems in a
     * production environment.
     * 
     * @param alwaysWaitForStreamThreads if true, wait always (original behavior); if false, do NOT wait when killed 
     */
    public void setAlwaysWaitForStreamThreads(boolean alwaysWaitForStreamThreads) {
        this.alwaysWaitForStreamThreads = alwaysWaitForStreamThreads;
    }

    
    /**
     * Start the <CODE>Thread</CODE>s.
     */
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
     * Stop pumping the streams.
     */
    public void stop() {
      stop(this.alwaysWaitForStreamThreads);
    }

    /**
     * Stop pumping the streams.
     * @param join if true, wait for the pump threads to complete, if false don't wait
     */
    public void stop(boolean join) {

        if (inputStreamPumper != null) {
            inputStreamPumper.stopProcessing();
        }

        if (join) {
            if (outputThread != null) {
                try {
                    outputThread.join();
                    outputThread = null;
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            if (errorThread != null) {
                try {
                    errorThread.join();
                    errorThread = null;
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            if (inputThread != null) {
                try {
                    inputThread.join();
                    inputThread = null;
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        else {
            // well, give each thread a chance to terminate itself before
            // we leave them alone
            if (outputThread != null) {
                outputThread.interrupt();
            }

            if (errorThread != null) {
                errorThread.interrupt();
            }

            if (inputThread != null) {
                inputThread.interrupt();
            }
        }

         if (err != null && err != out) {
             try {
                 err.flush();
             } catch (IOException e) {
                 String msg = "Got exception while flushing the error stream : " + e.getMessage();
                 DebugUtils.handleException(msg ,e);
             }
         }

         if (out != null) {
             try {
                 out.flush();
             } catch (IOException e) {
                 String msg = "Got exception while flushing the output stream";
                 DebugUtils.handleException(msg ,e);
             }
         }
    }

    /**
     * Get the error stream.
     * 
     * @return <CODE>OutputStream</CODE>.
     */
    protected OutputStream getErr() {
        return err;
    }

    /**
     * Get the output stream.
     * 
     * @return <CODE>OutputStream</CODE>.
     */
    protected OutputStream getOut() {
        return out;
    }

    /**
     * Create the pump to handle process output.
     * 
     * @param is
     *            the <CODE>InputStream</CODE>.
     * @param os
     *            the <CODE>OutputStream</CODE>.
     */
    protected void createProcessOutputPump(final InputStream is,
            final OutputStream os) {
        outputThread = createPump(is, os);
    }

    /**
     * Create the pump to handle error output.
     * 
     * @param is
     *            the <CODE>InputStream</CODE>.
     * @param os
     *            the <CODE>OutputStream</CODE>.
     */
    protected void createProcessErrorPump(final InputStream is,
            final OutputStream os) {
        errorThread = createPump(is, os);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the given
     * output stream.
     *
     * @param is the input stream to copy from
     * @param os the output stream to copy into
     * @return the stream pumper thread
     */
    protected Thread createPump(final InputStream is, final OutputStream os) {
        return createPump(is, os, false);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the given
     * output stream.
     *
     * @param is the input stream to copy from
     * @param os the output stream to copy into
     * @param closeWhenExhausted close the output stream when the input stream is exhausted
     * @return the stream pumper thread
     */
    protected Thread createPump(final InputStream is, final OutputStream os,
            final boolean closeWhenExhausted) {
        final Thread result = new Thread(new StreamPumper(is, os,
                closeWhenExhausted));
        result.setDaemon(true);
        return result;
    }


    /**
     * Creates a stream pumper to copy the given input stream to the given
     * output stream.
     *
     * @param is the System.in input stream to copy from
     * @param os the output stream to copy into
     * @return the stream pumper thread
     */
    private Thread createSystemInPump(InputStream is, OutputStream os) {
        inputStreamPumper = new InputStreamPumper(is, os);
        final Thread result = new Thread(inputStreamPumper);
        result.setDaemon(true);
        return result;
    }

}
