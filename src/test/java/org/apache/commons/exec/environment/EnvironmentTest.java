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

package org.apache.commons.exec.environment;

import java.io.IOException;
import java.util.Iterator;

import junit.framework.TestCase;

public class EnvironmentTest extends TestCase {

    private Environment getPrePopulatedEnvironment() {
        Environment env = Environment.createEnvironment();
        env.addVariable("foo", "bar");
        env.addVariable("xxx", "yyy");
        env.addVariable("abc", "def");

        return env;
    }

    public void testAddAndGet() {
        Environment env = Environment.createEnvironment();
        env.addVariable("foo", "bar");
        assertEquals(EnvironmentVariable
                .createEnvironmentVariable("foo", "bar"), env.get("foo"));
    }

    public void testSizeAndClear() {
        Environment env = getPrePopulatedEnvironment();

        assertEquals(3, env.size());
        env.clear();
        assertEquals(0, env.size());
    }

    public void testContainsKey() {
        Environment env = getPrePopulatedEnvironment();

        assertTrue(env.containsKey("foo"));
        assertFalse(env.containsKey("dummy"));
    }

    public void testContainsValue() {
        Environment env = getPrePopulatedEnvironment();

        assertTrue(env.containsValue(EnvironmentVariable
                .createEnvironmentVariable("foo", "bar")));
        assertFalse(env.containsValue(EnvironmentVariable
                .createEnvironmentVariable("dum", "my")));
    }

    public void testGetEnvironment() throws IOException {
        Environment env = Environment.getProcEnvironment();

        for (Iterator iter = env.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            System.out.println(env.get(key));
        }
    }
}
