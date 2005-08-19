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

import junit.framework.AssertionFailedError;

public final class TestUtil {

    private TestUtil() {

    }

    public static String resolveScriptForOS(String script) {
        if (OS.isFamilyWindows()) {
            return script + ".bat";
        } else if (OS.isFamilyUnix()) {
            return script + ".sh";
        } else {
            throw new AssertionFailedError("Test not supported for this OS");
        }
    }
}
