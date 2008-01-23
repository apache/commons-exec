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

/**
 * The callback handlers for asynchronous execution.
 *
 * @see org.apache.commons.exec.Executor#execute(CommandLine, java.util.Map, ExecuteResultHandler) 
 */
public interface ExecuteResultHandler {

  /**
   * The asynchronous exection completed.
   *
   * @param exitValue the exit value of the subprocess
   */
    void onProcessComplete(int exitValue);

  /**
   * The asynchronous exection failed.
   *
   * @param e the <code>ExecuteException</code> containing the root cause
   */
    void onProcessFailed(ExecuteException e);
}
