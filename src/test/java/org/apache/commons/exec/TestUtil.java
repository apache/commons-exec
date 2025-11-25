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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 */
public final class TestUtil {

    /**
     * Gets success and fail return codes used by the test scripts
     *
     * @return int array[2] = {ok, success}
     */
    public static int[] getTestScriptCodesForOS() {
        if (OS.isFamilyWindows() || OS.isFamilyUnix()) {
            return new int[] { 0, 1 };
        }
        if (OS.isFamilyOpenVms()) {
            return new int[] { 1, 2 };
        }
        fail("Test not supported for this OS");
        return null; // unreachable.
    }

    public static File resolveScriptFileForOS(final String script) {
        return resolveScriptPathForOS(script).toFile();
    }

    public static Path resolveScriptPathForOS(final String script) {
        if (OS.isFamilyWindows()) {
            return Paths.get(script + ".bat");
        }
        if (OS.isFamilyUnix()) {
            return Paths.get(script + ".sh");
        }
        if (OS.isFamilyOpenVms()) {
            return Paths.get(script + ".dcl");
        }
        fail("Test not supported for this OS");
        return null; // unreachable.
    }

    private TestUtil() {
    }

}
