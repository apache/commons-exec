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

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface Executor {

    /** Invalid exit code. * */
    public static final int INVALID_EXITVALUE = Integer.MAX_VALUE;

    
    /*
     * StreamHandlers are used for providing input, 
     * retriving the output. Also used for logging.  
     */
    ExecuteStreamHandler getStreamHandler();
    void setStreamHandler(ExecuteStreamHandler streamHandler);

    /*
     * Watchdog is used to kill of processes running, 
     * typically, too long time. 
     */
    ExecuteWatchdog getWatchdog();
    void setWatchdog(ExecuteWatchdog watchDog);
    
    File getWorkingDirectory();
    void setWorkingDirectory(File dir);

    /*
     * Methods for starting synchronous execution.
     * Returns process exit value
     */
    int execute(CommandLine command) throws ExecuteException, IOException; 
    int execute(CommandLine command, Map environment) throws ExecuteException, IOException; 
    
    /*
     * Methods for starting asynchronous execution. Result provided to callback handler
     */
    void execute(CommandLine command, ExecuteResultHandler handler) throws ExecuteException, IOException;
    void execute(CommandLine command, Map environment, ExecuteResultHandler handler) throws ExecuteException, IOException;

}
