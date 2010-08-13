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

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * An example based on the tutorial where the user can can safely play with
 * <ul>
 *  <li>blocking or non-blocking print jobs
 *  <li>with print job timeouts to trigger the <code>ExecuteWatchdog</code>
 *  <li>with the <code>exitValue</code> returned from the print script
 * </ul>
 */
public class TutorialTest extends TestCase {

    /** the directory to pick up the test scripts */
    private File testDir = new File("src/test/scripts");

    /** simulates a PDF print job */
    private File acroRd32Script = TestUtil.resolveScriptForOS(testDir + "/acrord32");

    public void testTutorialExample() throws Exception {

        File pdfFile = new File("./pom.xml");
        PrintResultFuture printResult;

        try {
            System.out.println("[main] Preparing print job ...");
            // printing takes around 3 seconds
            printResult = print(pdfFile, 10000, true);
            System.out.println("[main] Successfully sent the print job ...");
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("[main] Printing of the following document failed : " + pdfFile.getAbsolutePath());
            throw e;
        }

        // come back to check the print result
        System.out.println("[main] Test is exiting but waiting for the print job to finish...");
        printResult.waitFor();
    }

    /**
     * Simulate printing a PDF document.
     *
     * @param file the file to print
     * @param printJobTimeout the printJobTimeout (ms) before the watchdog terminates the print process
     * @param printInBackground printing done in the background or blocking
     * @return a print result handler (implementing a future)
     * @throws IOException the test failed
     */
    public PrintResultFuture print(File file, long printJobTimeout, boolean printInBackground)
            throws IOException {

        int exitValue;
        ExecuteWatchdog watchdog = null;
        PrintResultFuture resultFuture;

        // build up the command line to using a 'java.io.File'
        HashMap map = new HashMap();
        map.put("file", file);
        CommandLine commandLine = new CommandLine(acroRd32Script);
        commandLine.addArgument("/p");
        commandLine.addArgument("/h");
        commandLine.addArgument("${file}");
        commandLine.setSubstitutionMap(map);

        // create the executor and consider the exitValue '1' as success
        Executor executor = new DefaultExecutor();
        executor.setExitValue(1);
        
        // create a watchdog if requested
        if(printJobTimeout > 0) {

            watchdog = new ExecuteWatchdog(printJobTimeout);
            executor.setWatchdog(watchdog);
        }

        if(printInBackground) {
            System.out.println("[print] Executing non-blocking print job  ...");
            resultFuture = new PrintResultFuture(watchdog);
            executor.execute(commandLine, resultFuture);
        }
        else {
            System.out.println("[print] Executing blocking print job  ...");
            exitValue = executor.execute(commandLine);
            resultFuture = new PrintResultFuture(exitValue);
        }

        return resultFuture;
    }

    private class PrintResultFuture extends DefaultExecuteResultHandler {

        private ExecuteWatchdog watchdog;

        public PrintResultFuture(ExecuteWatchdog watchdog)
        {
            this.watchdog = watchdog;
        }

        public PrintResultFuture(int exitValue) {
            super.onProcessComplete(exitValue);
        }
        
        public void onProcessComplete(int exitValue) {
            super.onProcessComplete(exitValue);
            System.out.println("[resultHandler] The document was successfully printed ...");
        }

        public void onProcessFailed(ExecuteException e){
            super.onProcessFailed(e);
            if(watchdog != null && watchdog.killedProcess()) {
                System.err.println("[resultHandler] The print process timed out");
            }
            else {
                System.err.println("[resultHandler] The print process failed to do : " + e.getMessage());
            }
        }
    }
}