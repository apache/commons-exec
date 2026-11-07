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

package org.apache.commons.exec.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 */
class StringUtilTest {

    /**
     * Test a default string substitution, e.g. all placeholders are expanded.
     */
    @Test
    void testDefaultStringSubstitution() throws Exception {
        final Map<String, String> vars = new HashMap<>();
        vars.put("foo", "FOO");
        vars.put("bar", "BAR");

        assertEquals("This is a FOO & BAR test", StringUtils.stringSubstitution("This is a ${foo} & ${bar} test", vars, true).toString());
        assertEquals("This is a FOO & BAR test", StringUtils.stringSubstitution("This is a ${foo} & ${bar} test", vars, false).toString());
    }

    /**
     * Test an erroneous template.
     */
    @Test
    void testErroneousTemplate() throws Exception {
        final Map<String, String> vars = new HashMap<>();
        vars.put("foo", "FOO");

        assertEquals("This is a FOO & ${}} test", StringUtils.stringSubstitution("This is a ${foo} & ${}} test", vars, true).toString());
    }

    /**
     * Test a '$' at the end of the template, which must be copied as is.
     */
    @Test
    void testTrailingDollarSign() throws Exception {
        final Map<String, String> vars = new HashMap<>();
        vars.put("foo", "FOO");

        assertEquals("FOO costs 5$", StringUtils.stringSubstitution("${foo} costs 5$", vars, true).toString());
        assertEquals("FOO costs 5$", StringUtils.stringSubstitution("${foo} costs 5$", vars, false).toString());
        assertEquals("$", StringUtils.stringSubstitution("$", vars, true).toString());
        assertEquals("\\\\server\\c$", StringUtils.stringSubstitution("\\\\server\\c$", vars, true).toString());
    }

    /**
     * Test a variable without a closing delimiter at the end of the template.
     */
    @Test
    void testUnterminatedVariableAtEnd() throws Exception {
        final Map<String, String> vars = new HashMap<>();
        vars.put("foo", "FOO");

        assertThrows(IllegalArgumentException.class, () -> StringUtils.stringSubstitution("${foo", vars, true));
        assertThrows(IllegalArgumentException.class, () -> StringUtils.stringSubstitution("${", vars, true));
    }

    /**
     * Test an incomplete string substitution where not all placeholders are expanded.
     */
    @Test
    void testIncompleteSubstitution() throws Exception {

        final Map<String, String> vars = new HashMap<>();
        vars.put("foo", "FOO");

        assertEquals("This is a FOO & ${bar} test", StringUtils.stringSubstitution("This is a ${foo} & ${bar} test", vars, true).toString());

        try {
            StringUtils.stringSubstitution("This is a ${foo} & ${bar} test", vars, false).toString();
            fail();
        } catch (final RuntimeException e) {
            // nothing to do
        }
    }

    /**
     * Test no string substitution
     */
    @Test
    void testNoStringSubstitution() throws Exception {
        final Map<String, String> vars = new HashMap<>();
        vars.put("foo", "FOO");
        vars.put("bar", "BAR");

        assertEquals("This is a FOO & BAR test", StringUtils.stringSubstitution("This is a FOO & BAR test", vars, true).toString());
    }

    @Test
    void testQuoteArgument() throws Exception {
        assertEquals("hi", StringUtils.quoteArgument("'hi'"));
        assertEquals("hi", StringUtils.quoteArgument("\"hi\""));

        // cmd : bash -c "echo 'hi'"
        assertEquals("\"echo 'hi'\"", StringUtils.quoteArgument("echo 'hi'"));
        assertEquals("'echo \"hi\"'", StringUtils.quoteArgument("echo \"hi\""));

        assertThrows(IllegalArgumentException.class, () -> StringUtils.quoteArgument("echo \"hi 'world'\""),
                "Can't handle single and double quotes in same argument");
    }

}
