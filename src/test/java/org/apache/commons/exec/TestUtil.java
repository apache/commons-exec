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

import org.opentest4j.AssertionFailedError;

import java.io.File;


/**
 */
public final class TestUtil {

    private TestUtil() {
    }

    public static File resolveScriptForOS(final String script) {
        if (OS.isFamilyWindows()) {
            return new File(script + ".bat");
        }
        if (OS.isFamilyUnix()) {
            return new File(script + ".sh");
        }
        if (OS.isFamilyOpenVms()) {
            return new File(script + ".dcl");
        }
        throw new AssertionFailedError("Test not supported for this OS");
    }

    /**
     * Get success and fail return codes used by the test scripts
     * @return int array[2] = {ok, success}
     */
    public static int[] getTestScriptCodesForOS() {
        if (OS.isFamilyWindows()) {
            return new int[]{0,1};
        }
        if (OS.isFamilyUnix()) {
            return new int[]{0,1};
        }
        if (OS.isFamilyOpenVms()) {
            return new int[]{1,2};
        }
        throw new AssertionFailedError("Test not supported for this OS");
    }

}
