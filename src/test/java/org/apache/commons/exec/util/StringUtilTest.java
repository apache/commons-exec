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

package org.apache.commons.exec.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @version $Id$
 */
public class StringUtilTest {
    /**
     * Test no string substitution
     */
    @Test
    public void testNoStringSubstitution() throws Exception
    {
        final Map<String, String> vars = new HashMap<String, String>();
        vars.put("foo", "FOO");
        vars.put("bar", "BAR");

        assertEquals("This is a FOO & BAR test", StringUtils.stringSubstitution("This is a FOO & BAR test", vars, true).toString());
    }

    /**
     * Test a default string substitution, e.g. all placeholders
     * are expanded.
     */
    @Test
    public void testDefaultStringSubstitution() throws Exception 
    {
        final Map<String, String> vars = new HashMap<String, String>();
        vars.put("foo", "FOO");
        vars.put("bar", "BAR");

        assertEquals("This is a FOO & BAR test", StringUtils.stringSubstitution("This is a ${foo} & ${bar} test", vars, true).toString());
        assertEquals("This is a FOO & BAR test", StringUtils.stringSubstitution("This is a ${foo} & ${bar} test", vars, false).toString());
    }

    /**
     * Test an incomplete string substitution where not all placeholders
     * are expanded.
     */
    @Test
    public void testIncompleteSubstitution() throws Exception {

        final Map<String, String> vars = new HashMap<String, String>();
        vars.put("foo", "FOO");

        assertEquals("This is a FOO & ${bar} test",  StringUtils.stringSubstitution("This is a ${foo} & ${bar} test", vars, true).toString());

        try
        {
            StringUtils.stringSubstitution("This is a ${foo} & ${bar} test", vars, false).toString();
            fail();
        }
        catch (final RuntimeException e)
        {
            // nothing to do
        }
    }

    /**
     * Test a erroneous template.
     */
    @Test
    public void testErroneousTemplate() throws Exception
    {
        final Map<String, String> vars = new HashMap<String, String>();
        vars.put("foo", "FOO");

        assertEquals("This is a FOO & ${}} test",  StringUtils.stringSubstitution("This is a ${foo} & ${}} test", vars, true).toString());
    }
}