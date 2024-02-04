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

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;

/**
 * @see <a href="https://issues.apache.org/jira/browse/EXEC-62">EXEC-62</a>
 */
public class Exec62Test {
    private Path outputFile;

    private void execute(final String scriptName) throws Exception {
        final ExecuteWatchdog watchdog = new ExecuteWatchdog(4000);
        final CommandLine commandLine = new CommandLine("/bin/sh");
        final File testScript = TestUtil.resolveScriptForOS("./src/test/scripts/issues/" + scriptName);

        commandLine.addArgument(testScript.getAbsolutePath());

        final DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.setExitValues(null); // ignore exit values
        executor.setWatchdog(watchdog);

        try (OutputStream fos = Files.newOutputStream(outputFile)) {
            final PumpStreamHandler streamHandler = new PumpStreamHandler(fos);
            executor.setStreamHandler(streamHandler);
            executor.execute(commandLine);

            if (watchdog.killedProcess()) {
                throw new TimeoutException(String.format("Transcode process was killed on timeout %1$s ms, command line %2$s", 4000, commandLine.toString()));
            }
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        outputFile = Files.createTempFile("foo", ".log");
    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.delete(outputFile);
    }

    @Disabled("Test behaves differently between macOS X and Linux - don't know why")
    @Test
    @DisabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testMe() throws Exception {
        execute("exec-62");
    }
}
