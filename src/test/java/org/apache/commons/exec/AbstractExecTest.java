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

package org.apache.commons.exec;

import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.File;

public abstract class AbstractExecTest {

    public static final int TEST_TIMEOUT = 15000;
    public static final int WATCHDOG_TIMEOUT = 3000;

    private static final String OS_NAME = System.getProperty("os.name");

    private final File testDir = new File("src/test/scripts");

    @Rule public final TestName name = new TestName();

    /**
     * Resolve the OS-specific test file to execute.
     */
    protected File resolveTestScript(final String baseName) {
        final File result = TestUtil.resolveScriptForOS(testDir + "/" + baseName);
        if (!result.exists()) {
            throw new IllegalArgumentException("Unable to find the following file: " + result.getAbsolutePath());
        }
        return result;
    }

    /**
     * Resolve the OS-specific test file to execute.
     */
    protected File resolveTestScript(final String directoryName, final String baseName) {
        final File result = TestUtil.resolveScriptForOS(testDir + "/" + directoryName + "/" + baseName);
        if (!result.exists()) {
            throw new IllegalArgumentException("Unable to find the following file: " + result.getAbsolutePath());
        }
        return result;
    }

    /**
     * Get the name of the currently executed test.
     */
    protected String getName() {
        return name.getMethodName();
    }

    protected String testNotSupportedForCurrentOperatingSystem() {
        final String msg = String.format("The test '%s' is not possible for OS : %s", name.getMethodName(), OS_NAME);
        System.out.println(msg);
        return msg;
    }

    protected String testIsBrokenForCurrentOperatingSystem() {
        final String msg = String.format("The test '%s' is broken for OS : %s", name.getMethodName(), OS_NAME);
        System.err.println(msg);
        return msg;
    }

}
