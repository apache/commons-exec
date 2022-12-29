/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.exec.issues;

import org.apache.commons.exec.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @see <a href="https://issues.apache.org/jira/browse/EXEC-62">EXEC-62</a>
 */
public class Exec62Test
{
    private File outputFile;

    @BeforeEach
    public void setUp() throws Exception {
        outputFile = File.createTempFile("foo", ".log");
    }

    @AfterEach
    public void tearDown() throws Exception {
        outputFile.delete();
    }

    @Disabled("Test behaves differently between Mac OS X and Linux - don't know why")
    @Test
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void testMe() throws Exception {
        if(OS.isFamilyUnix()) {
            assertThrows(TimeoutException.class, () -> execute ("exec-62"));
        }
    }

    private void execute (final String scriptName) throws Exception {
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(4000);
        final CommandLine commandLine = new CommandLine("/bin/sh");
        final File testScript = TestUtil.resolveScriptForOS("./src/test/scripts/issues/" + scriptName);

        commandLine.addArgument(testScript.getAbsolutePath());

        final DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValues(null); // ignore exit values
        executor.setWatchdog(watchdog);

        final FileOutputStream fos = new FileOutputStream(outputFile);
        final PumpStreamHandler streamHandler = new PumpStreamHandler(fos);
        executor.setStreamHandler(streamHandler);
        executor.execute(commandLine);

        if (watchdog.killedProcess()) {
            throw new TimeoutException(String.format("Transcode process was killed on timeout %1$s ms, command line %2$s", 4000, commandLine.toString()));
        }
    }
}


