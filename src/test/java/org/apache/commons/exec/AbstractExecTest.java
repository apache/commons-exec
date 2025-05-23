/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.exec;

import java.io.File;

/**
 * Abstracts tests.
 */
public abstract class AbstractExecTest {

    /**
     * Default test timeout in milliseconds.
     */
    public static final int TEST_TIMEOUT = 15_000;

    /**
     * Watchdog timeout in milliseconds.
     */
    public static final int WATCHDOG_TIMEOUT = 3_000;

    private final File testDir = new File("src/test/scripts");

    /**
     * Resolve the OS-specific test file to execute.
     */
    protected File resolveTestScript(final String baseName) {
        final File result = TestUtil.resolveScriptFileForOS(testDir + "/" + baseName);
        if (!result.exists()) {
            throw new IllegalArgumentException("Unable to find the following file: " + result.getAbsolutePath());
        }
        return result;
    }

    /**
     * Resolve the OS-specific test file to execute.
     */
    protected File resolveTestScript(final String directoryName, final String baseName) {
        final File result = TestUtil.resolveScriptFileForOS(testDir + "/" + directoryName + "/" + baseName);
        if (!result.exists()) {
            throw new IllegalArgumentException("Unable to find the following file: " + result.getAbsolutePath());
        }
        return result;
    }
}
