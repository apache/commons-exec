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

import java.io.IOException;

/**
 * Logs standard output and error of a subprocess to the log system of ant.
 */
public class LogStreamHandler extends PumpStreamHandler {

    /**
     * Creates log stream handler.
     * 
     * @param outlevel
     *            the loglevel used to log standard output
     * @param errlevel
     *            the loglevel used to log standard error
     */
    public LogStreamHandler(final int outlevel, final int errlevel) {
        super(new LogOutputStream(outlevel), new LogOutputStream(errlevel));
    }

    /**
     * Stop the log stream handler.
     */
    public void stop() {
        super.stop();
        try {
            getErr().close();
            getOut().close();
        } catch (IOException e) {
            // ignore
        }
    }
}
