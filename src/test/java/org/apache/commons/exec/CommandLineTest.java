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

import junit.framework.TestCase;

public class CommandLineTest extends TestCase {

    public void testSetExecutable() {
        CommandLine cmdl = new CommandLine();
        cmdl.setExecutable("test");
        assertEquals("test", cmdl.toString());
    }

    public void testSetArguments() {
        CommandLine cmdl = new CommandLine();
        cmdl.setExecutable("test");
        cmdl.addArgument("foo");
        cmdl.addArgument("bar");
        assertEquals("test foo bar", cmdl.toString());
    }

    public void testSetArgumentsWithSpace() {
        CommandLine cmdl = new CommandLine();
        cmdl.setExecutable("test");
        cmdl.addArgument("foo");
        cmdl.addArgument("ba r");
        assertEquals("test foo \"ba r\"", cmdl.toString());
    }

    public void testSetArgumentsWithQuote() {
        CommandLine cmdl = new CommandLine();
        cmdl.setExecutable("test");
        cmdl.addArgument("foo");
        cmdl.addArgument("ba\"r");
        assertEquals("test foo 'ba\"r'", cmdl.toString());
    }

    public void testSetArgumentsWithSingleQuote() {
        CommandLine cmdl = new CommandLine();
        cmdl.setExecutable("test");
        cmdl.addArgument("foo");
        cmdl.addArgument("ba'r");
        assertEquals("test foo \"ba'r\"", cmdl.toString());
    }
}
