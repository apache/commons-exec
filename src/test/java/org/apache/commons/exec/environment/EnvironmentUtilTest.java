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

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.exec.OS;
import org.junit.Test;

/**
 * @version $Id$
 */
public class EnvironmentUtilTest {

    /**
     * Tests the behaviour of the EnvironmentUtils.toStrings()
     * when using a <code>null</code> environment.
     */
    @Test
    public void testToStrings() {
        // check for a non-existing environment when passing null
        assertNull(EnvironmentUtils.toStrings(null));
        // check for an environment when filling in two variables
        final Map env = new HashMap();
        assertArrayEquals(new String[0], EnvironmentUtils.toStrings(env));
        env.put("foo2", "bar2");
        env.put("foo", "bar");
        final String[] envStrings = EnvironmentUtils.toStrings(env);
        final String[] expected = new String[]{"foo2=bar2", "foo=bar"};
        assertArrayEquals(expected, envStrings);
    }

    /**
     * Test to access the environment variables of the current
     * process. Please note that this test does not run on
     * java-gjc.
     *
     * @throws IOException the test failed
     */
    @Test
    public void testGetProcEnvironment() throws IOException {
        final Map procEnvironment = EnvironmentUtils.getProcEnvironment();
        // we assume that there is at least one environment variable
        // for this process, i.e. $JAVA_HOME
        assertTrue("Expecting non-zero environment size", procEnvironment.size() > 0);
        final String[] envArgs = EnvironmentUtils.toStrings(procEnvironment);
        for (int i=0; i<envArgs.length; i++) {
            assertNotNull("Entry "+i+" should not be null",envArgs[i]);
            assertTrue("Entry "+i+" should not be empty",envArgs[i].length() > 0);
            // System.out.println(envArgs[i]);
        }
    }

    /**
     * On Windows platforms test that accessing environment variables
     * can be done in a case-insensitive way, e.g. "PATH", "Path" and
     * "path" would reference the same environment variable.
     *
     * @throws IOException the test failed
     */
    @Test
    public void testGetProcEnvironmentCaseInsensitiveLookup() throws IOException {
        // run tests only on windows platforms
        if (!OS.isFamilyWindows()) {
            return;
        }

        // ensure that we have the same value for upper and lowercase keys
        final Map procEnvironment = EnvironmentUtils.getProcEnvironment();
        for (final Iterator it = procEnvironment.entrySet().iterator(); it.hasNext();) {
            final Map.Entry entry = (Map.Entry) it.next();
            final String key = (String) entry.getKey();
            final String value = (String) entry.getValue();
            assertEquals(value, procEnvironment.get(key.toLowerCase(Locale.ENGLISH)));
            assertEquals(value, procEnvironment.get(key.toUpperCase(Locale.ENGLISH)));
        }

        // add an environment variable and check access
        EnvironmentUtils.addVariableToEnvironment(procEnvironment, "foo=bar");
        assertEquals("bar", procEnvironment.get("FOO"));
        assertEquals("bar", procEnvironment.get("Foo"));
        assertEquals("bar", procEnvironment.get("foo"));
    }

    /**
     * Accessing environment variables is case-sensitive or not depending
     * on the operating system but the values of the environment variable
     * are always case-sensitive. So make sure that this assumption holds
     * on all operating systems.
     *
     * @throws Exception the test failed
     */
    @Test
    public void testCaseInsensitiveVariableLookup() throws Exception {
        final Map procEnvironment = EnvironmentUtils.getProcEnvironment();
        // Check that case is preserved for values
        EnvironmentUtils.addVariableToEnvironment(procEnvironment, "foo=bAr");
        assertEquals("bAr", procEnvironment.get("foo"));
    }

}
