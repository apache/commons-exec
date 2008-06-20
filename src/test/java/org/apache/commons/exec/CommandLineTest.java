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

import java.util.Arrays;
import java.util.HashMap;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class CommandLineTest extends TestCase {

    private void assertEquals(String[] expected, String[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionFailedError("Arrays not equal");
        }
    }

    public void testExecutable() {
        CommandLine cmdl = new CommandLine("test");
        assertEquals("test", cmdl.toString());
        assertEquals(new String[] {"test"}, cmdl.toStrings());
        assertEquals("test", cmdl.getExecutable());
        assertTrue(cmdl.getArguments().length == 0);
    }

    public void testExecutableZeroLengthString() {
        try {
            CommandLine cmdl = new CommandLine("");
            fail("Must throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testExecutableWhitespaceString() {
        try {
            CommandLine cmdl = new CommandLine("   ");
            fail("Must throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testNullExecutable() {
        try {
            CommandLine cmdl = new CommandLine((String)null);
            fail("Must throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testAddArgument() {
        CommandLine cmdl = new CommandLine("test");

        cmdl.addArgument("foo");
        cmdl.addArgument("bar");
        assertEquals("test foo bar", cmdl.toString());
        assertEquals(new String[] {"test", "foo", "bar"}, cmdl.toStrings());
    }

    public void testAddNullArgument() {
        CommandLine cmdl = new CommandLine("test");

        cmdl.addArgument(null);
        assertEquals("test", cmdl.toString());
        assertEquals(new String[] {"test"}, cmdl.toStrings());
    }

    public void testAddArgumentWithSpace() {
        CommandLine cmdl = new CommandLine("test");
        cmdl.addArgument("foo");
        cmdl.addArgument("ba r");
        assertEquals("test foo \"ba r\"", cmdl.toString());
        assertEquals(new String[] {"test", "foo", "\"ba r\""}, cmdl.toStrings());
    }

    public void testAddArgumentWithQuote() {
        CommandLine cmdl = new CommandLine("test");
        cmdl.addArgument("foo");
        cmdl.addArgument("ba\"r");
        assertEquals("test foo 'ba\"r'", cmdl.toString());
        assertEquals(new String[] {"test", "foo", "'ba\"r'"}, cmdl.toStrings());
    }

    public void testAddArgumentWithQuotesAround() {
        CommandLine cmdl = new CommandLine("test");
        cmdl.addArgument("\'foo\'");
        cmdl.addArgument("\"bar\"");
        cmdl.addArgument("\"fe z\"");
        assertEquals("test foo bar \"fe z\"", cmdl.toString());
        assertEquals(new String[] {"test", "foo", "bar", "\"fe z\""}, cmdl.toStrings());
    }

    public void testAddArgumentWithSingleQuote() {
        CommandLine cmdl = new CommandLine("test");

        cmdl.addArgument("foo");
        cmdl.addArgument("ba'r");
        assertEquals("test foo \"ba'r\"", cmdl.toString());
        assertEquals(new String[] {"test", "foo", "\"ba\'r\""}, cmdl
                .toStrings());
    }

    public void testAddArgumentWithBothQuotes() {
        CommandLine cmdl = new CommandLine("test");

        try {
            cmdl.addArgument("b\"a'r");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // OK, expected
        }
    }

    public void testAddArguments() {
        CommandLine cmdl = new CommandLine("test");
        cmdl.addArguments("foo bar");
        assertEquals("test foo bar", cmdl.toString());
        assertEquals(new String[] {"test", "foo", "bar"}, cmdl.toStrings());
    }

    public void testAddArgumentsWithQuotes() {
        CommandLine cmdl = new CommandLine("test");
        cmdl.addArguments("'foo' \"bar\"");
        assertEquals("test foo bar", cmdl.toString());
        assertEquals(new String[] {"test", "foo", "bar"}, cmdl.toStrings());
    }

    public void testAddArgumentsWithQuotesAndSpaces() {
        CommandLine cmdl = new CommandLine("test");
        cmdl.addArguments("'fo o' \"ba r\"");
        assertEquals("test \"fo o\" \"ba r\"", cmdl.toString());
        assertEquals(new String[] {"test", "\"fo o\"", "\"ba r\""}, cmdl
                .toStrings());
    }

    public void testAddArgumentsArray() {
        CommandLine cmdl = new CommandLine("test");
        cmdl.addArguments(new String[] {"foo", "bar"});
        assertEquals("test foo bar", cmdl.toString());
        assertEquals(new String[] {"test", "foo", "bar"}, cmdl.toStrings());
    }

    public void testAddArgumentsArrayNull() {
        CommandLine cmdl = new CommandLine("test");
        cmdl.addArguments((String[]) null);
        assertEquals("test", cmdl.toString());
        assertEquals(new String[] {"test"}, cmdl.toStrings());
    }

    public void testParseCommandLine() {
        CommandLine cmdl = CommandLine.parse("test foo bar");
        assertEquals("test foo bar", cmdl.toString());
        assertEquals(new String[] {"test", "foo", "bar"}, cmdl.toStrings());
    }

    public void testParseCommandLineWithQuotes() {
        CommandLine cmdl = CommandLine.parse("test \"foo\" \'ba r\'");
        assertEquals("test foo \"ba r\"", cmdl.toString());
        assertEquals(new String[] {"test", "foo", "\"ba r\""}, cmdl.toStrings());
    }

    public void testParseCommandLineWithUnevenQuotes() {
        try {
            CommandLine.parse("test \"foo bar");
            fail("IllegalArgumentException must be thrown due to uneven quotes");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testParseCommandLineWithNull() {
        try {
            CommandLine.parse(null);
            fail("IllegalArgumentException must be thrown due to incorrect command line");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testParseCommandLineWithOnlyWhitespace() {
        try {
            CommandLine.parse("  ");
            fail("IllegalArgumentException must be thrown due to incorrect command line");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }


    /**
     * A command line parsing puzzle from Tino Schoellhorn - ImageMagix expects
     * a "500x>" parameter (including quotes) and it is simply not possible to
     * do that withoud adding a space, e.g. "500x> ".
     */
    public void testParseComplexCommandLine1() throws Exception {
        HashMap substitutionMap = new HashMap();
        substitutionMap.put("in", "source.jpg");
        substitutionMap.put("out", "target.jpg");
        CommandLine cmdl = CommandLine.parse("cmd /C convert ${in} -resize \"\'500x> \'\" ${out}", substitutionMap);
        assertEquals("cmd /C convert source.jpg -resize \"500x> \" target.jpg", cmdl.toString());
        return;
    }

   /**
    * Create a command line with pre-quoted strings to test SANDBOX-192,
    * e.g. "runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\""
    */
    public void testComplexAddArgument() {
        CommandLine cmdl = new CommandLine("runMemorySud.cmd");
        cmdl.addArgument("10", false);
        cmdl.addArgument("30", false);
        cmdl.addArgument("-XX:+UseParallelGC", false);
        cmdl.addArgument("\"-XX:ParallelGCThreads=2\"", false);
        assertEquals("runMemorySud.cmd 10 30 -XX:+UseParallelGC \"-XX:ParallelGCThreads=2\"", cmdl.toString());
        assertEquals(new String[] {"runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\""}, cmdl.toStrings());
    }

    /**
     * Create a command line with pre-quoted strings to test SANDBOX-192,
     * e.g. "runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\""
     */
     public void testComplexAddArguments1() {
         CommandLine cmdl = new CommandLine("runMemorySud.cmd");
         cmdl.addArguments(new String[] {"10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\""}, false);
         assertEquals("runMemorySud.cmd 10 30 -XX:+UseParallelGC \"-XX:ParallelGCThreads=2\"", cmdl.toString());
         assertEquals(new String[] {"runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\""}, cmdl.toStrings());
     }

    /**
     * Create a command line with pre-quoted strings to test SANDBOX-192,
     * e.g. "runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\""
     * Please not that we re forced to add additional single quotes to get the test working -
     * don't know if this is a bug or a feature.
     */
     public void testComplexAddArguments2() {
         CommandLine cmdl = new CommandLine("runMemorySud.cmd");
         cmdl.addArguments("10 30 -XX:+UseParallelGC '\"-XX:ParallelGCThreads=2\"'", false);
         assertEquals("runMemorySud.cmd 10 30 -XX:+UseParallelGC \"-XX:ParallelGCThreads=2\"", cmdl.toString());
         assertEquals(new String[] {"runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\""}, cmdl.toStrings());
     }

    /**
     * Test expanding the command line based on a user-supplied map.
     */
    public void testCommandLineParsingWithExpansion1() {

        CommandLine cmdl = null;

        HashMap substitutionMap = new HashMap();
        substitutionMap.put("JAVA_HOME", "/usr/local/java");
        substitutionMap.put("appMainClass", "foo.bar.Main");

        HashMap incompleteMap = new HashMap();
        incompleteMap.put("JAVA_HOME", "/usr/local/java");

        // do not pass substitution map
        cmdl = CommandLine.parse("${JAVA_HOME}/bin/java ${appMainClass}");
        assertTrue(cmdl.getExecutable().indexOf("${JAVA_HOME}") == 0 );
        assertEquals(new String[] {"${appMainClass}"}, cmdl.getArguments());

        // pass arguments with an empty map
        cmdl = CommandLine.parse("${JAVA_HOME}/bin/java ${appMainClass}", new HashMap());
        assertTrue(cmdl.getExecutable().indexOf("${JAVA_HOME}") == 0 );
        assertEquals(new String[] {"${appMainClass}"}, cmdl.getArguments());

        // pass an complete substitution map
        cmdl = CommandLine.parse("${JAVA_HOME}/bin/java ${appMainClass}", substitutionMap);
        assertTrue(cmdl.getExecutable().indexOf("${JAVA_HOME}") < 0 );
        assertTrue(cmdl.getExecutable().indexOf("local") > 0 );
        assertEquals(new String[] {"foo.bar.Main"}, cmdl.getArguments());

        // pass an incomplete substitution map resulting in unresolved variables
        cmdl = CommandLine.parse("${JAVA_HOME}/bin/java ${appMainClass}", incompleteMap);
        assertTrue(cmdl.getExecutable().indexOf("${JAVA_HOME}") < 0 );
        assertTrue(cmdl.getExecutable().indexOf("local") > 0 );
        assertEquals(new String[] {"${appMainClass}"}, cmdl.getArguments());
    }

    /**
     * Test expanding the command line based on a user-supplied map.
     */
    public void testCommandLineParsingWithExpansion2() throws Exception {

        CommandLine cmdl = null;
        String[] result = null;

        HashMap substitutionMap = new HashMap();
        substitutionMap.put("JAVA_HOME", "C:\\Programme\\jdk1.5.0_12");
        substitutionMap.put("appMainClass", "foo.bar.Main");

        // build the command line
        cmdl = new CommandLine("${JAVA_HOME}\\bin\\java");
        cmdl.addArguments("-class");
        cmdl.addArguments("${appMainClass}");
        cmdl.addArguments("${file}");

        // build the first command line
        substitutionMap.put("file", "C:\\Document And Settings\\documents\\432431.pdf");
        cmdl.setSubstitutionMap(substitutionMap);
        result = cmdl.toStrings();
        assertEquals(new String[] {"C:\\Programme\\jdk1.5.0_12/bin/java", "-class", "foo.bar.Main", "C:\\Document And Settings\\documents\\432431.pdf"}, result);

        // build the second command line
        substitutionMap.put("file", "C:\\Document And Settings\\documents\\432432.pdf");        
        cmdl.setSubstitutionMap(substitutionMap);
        result = cmdl.toStrings();
        assertEquals(new String[] {"C:\\Programme\\jdk1.5.0_12/bin/java", "-class", "foo.bar.Main", "C:\\Document And Settings\\documents\\432432.pdf"}, result);
    }
}
