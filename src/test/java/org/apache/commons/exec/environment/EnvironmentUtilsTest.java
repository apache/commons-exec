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

package org.apache.commons.exec.environment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.exec.OS;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link EnvironmentUtils}.
 */
class EnvironmentUtilsTest {

    /**
     * Accessing environment variables is case-sensitive or not depending on the operating system but the values of the environment variable are always
     * case-sensitive. So make sure that this assumption holds on all operating systems.
     *
     * @throws Exception the test failed
     */
    @Test
    void testCaseInsensitiveVariableLookup() throws Exception {
        final Map<String, String> procEnvironment = EnvironmentUtils.getProcEnvironment();
        // Check that case is preserved for values
        EnvironmentUtils.addVariableToEnvironment(procEnvironment, "foo=bAr");
        assertEquals("bAr", procEnvironment.get("foo"));
    }

    /**
     * Test to access the environment variables of the current process. Please note that this test does not run on java-gjc.
     *
     * @throws IOException the test failed
     */
    @Test
    void testGetProcEnvironment() throws IOException {
        final Map<String, String> procEnvironment = EnvironmentUtils.getProcEnvironment();
        // we assume that there is at least one environment variable
        // for this process, i.e. $JAVA_HOME
        assertFalse(procEnvironment.isEmpty(), "Expecting non-zero environment size");
        final String[] envArgs = EnvironmentUtils.toStrings(procEnvironment);
        for (int i = 0; i < envArgs.length; i++) {
            assertNotNull(envArgs[i], "Entry " + i + " should not be null");
            assertFalse(envArgs[i].isEmpty(), "Entry " + i + " should not be empty");
            // System.out.println(envArgs[i]);
        }
    }

    /**
     * On Windows platforms test that accessing environment variables can be done in a case-insensitive way, e.g. "PATH", "Path" and "path" would reference the
     * same environment variable.
     *
     * @throws IOException the test failed
     */
    @Test
    void testGetProcEnvironmentCaseInsensitiveLookup() throws IOException {
        // run tests only on windows platforms
        if (!OS.isFamilyWindows()) {
            return;
        }

        // ensure that we have the same value for upper and lowercase keys
        final Map<String, String> procEnvironment = EnvironmentUtils.getProcEnvironment();
        for (final Entry<String, String> entry : procEnvironment.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            assertEquals(value, procEnvironment.get(StringUtils.toRootLowerCase(key)));
            assertEquals(value, procEnvironment.get(StringUtils.toRootUpperCase(key)));
        }

        // add an environment variable and check access
        EnvironmentUtils.addVariableToEnvironment(procEnvironment, "foo=bar");
        assertEquals("bar", procEnvironment.get("FOO"));
        assertEquals("bar", procEnvironment.get("Foo"));
        assertEquals("bar", procEnvironment.get("foo"));
    }

    /**
     * Tests the behavior of the EnvironmentUtils.toStrings() when using a {@code null} environment.
     */
    @Test
    void testToStrings() {
        // check for a non-existing environment when passing null
        assertNull(EnvironmentUtils.toStrings(null));
        // check for an environment when filling in two variables
        final Map<String, String> env = new HashMap<>();
        assertArrayEquals(new String[0], EnvironmentUtils.toStrings(env));
        env.put("foo2", "bar2");
        env.put("foo", "bar");
        final String[] envStrings = EnvironmentUtils.toStrings(env);
        final String[] expected = { "foo2=bar2", "foo=bar" };
        // ensure the result does not depend on the hash ordering
        Arrays.sort(expected);
        Arrays.sort(envStrings);
        assertArrayEquals(expected, envStrings);
    }

    /**
     * Tests the behavior of the EnvironmentUtils.toStrings() when using a {@code null} key given to the map.
     */
    @Test
    void testToStringWithNullKey() {
        final Map<String, String> env = new HashMap<>();
        env.put(null, "TheNullKey");
        final String[] strings = EnvironmentUtils.toStrings(env);
        assertEquals(1, strings.length);
        assertEquals("=TheNullKey", strings[0]);
    }

    /**
     * Tests the behavior of the EnvironmentUtils.toStrings() when using a {@code null} value given to the map.
     */
    @Test
    void testToStringWithNullValue() {
        final Map<String, String> env = new HashMap<>();
        env.put("key", null);
        final String[] strings = EnvironmentUtils.toStrings(env);
        assertEquals(1, strings.length);
        assertEquals("key=", strings[0]);
    }

}
