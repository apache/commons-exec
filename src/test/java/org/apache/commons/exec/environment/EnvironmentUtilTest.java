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

package org.apache.commons.exec.environment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.exec.TestUtil;

public class EnvironmentUtilTest extends TestCase {

    public void testToStrings() throws IOException {
        Map env = new HashMap();
        
        env.put("foo2", "bar2");
        env.put("foo", "bar");

        String[] envStrings = EnvironmentUtil.toStrings(env);

        String[] expected = new String[]{"foo=bar", "foo2=bar2"};
        
        
        TestUtil.assertEquals(expected, envStrings, false);
    }

    /**
     * Test to access the environment variables of the current
     * process. Please note that this test does not run on
     * java-gjc 
     */
    public void testGetProcEnvironment() throws IOException {
        Map procEnvironment = EnvironmentUtil.getProcEnvironment();
        // we assume that there is at least one environment variable
        // for this process
        assertTrue(procEnvironment.size() > 0);
        String[] envArgs = EnvironmentUtil.toStrings(procEnvironment);
        for(int i=0; i<envArgs.length; i++) {
            assertNotNull(envArgs[i]);
            assertTrue(envArgs[i].length() > 0);
            System.out.println(envArgs[i]);
        }
    }
}
