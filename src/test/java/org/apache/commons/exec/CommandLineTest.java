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

package org.apache.commons.exec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.util.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 */
class CommandLineTest {

    /** Directory the child process writes the arguments it received into. */
    private static final Path RECORDED_ARGS_DIR = Paths.get("target", "quoted-arguments");

    /**
     * Runs the current JVM as a child process with the given arguments and returns the arguments the child process actually received. The child process writes
     * each argument to its own file, so the comparison is not affected by the console encoding.
     */
    private static List<String> runChildProcess(final String... arguments) throws Exception {
        final Path outDir = RECORDED_ARGS_DIR.toAbsolutePath();
        if (Files.isDirectory(outDir)) {
            try (java.util.stream.Stream<Path> stream = Files.list(outDir)) {
                for (final Path path : stream.toArray(Path[]::new)) {
                    Files.deleteIfExists(path);
                }
            }
        }
        final Path java = Paths.get(System.getProperty("java.home"), "bin", "java");
        final CommandLine commandLine = new CommandLine(java);
        commandLine.addArgument("-cp");
        commandLine.addArgument(System.getProperty("java.class.path"));
        commandLine.addArgument(QuotedArgumentRecorder.class.getName());
        commandLine.addArgument(outDir.toString());
        commandLine.addArguments(arguments);
        new DefaultExecutor().execute(commandLine);
        final List<String> received = new ArrayList<>();
        for (int i = 0;; i++) {
            final Path file = outDir.resolve(String.format("%03d.txt", i));
            if (!Files.exists(file)) {
                break;
            }
            received.add(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        }
        assertEquals(String.valueOf(received.size()), new String(Files.readAllBytes(outDir.resolve("count.txt")), StandardCharsets.UTF_8),
                "the child process recorded a different number of arguments than this test read back");
        return received;
    }

    /**
     * An argument that ends in a backslash must survive being quoted and passed to a child process. A backslash in front of the closing double quote escapes
     * that quote, which used to turn {@code C:\Program Files\} into {@code C:\Program Files"} in the child process.
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testAddArgumentEndingInBackslashIsDeliveredUnchanged() throws Exception {
        final String directoryWithSpace = "C:\\Program Files\\";
        final String pathWithSpace = "C:\\Program Files\\Apache Software Foundation\\";
        assertEquals(Arrays.asList(directoryWithSpace, "plain"),
                runChildProcess(directoryWithSpace, "plain"), "argument ending with a backslash was changed while being passed to the child process");
        assertEquals(Arrays.asList(pathWithSpace), runChildProcess(pathWithSpace),
                "path ending with a backslash was changed while being passed to the child process");
    }

    /**
     * An argument without spaces is not quoted, so its trailing backslashes stay as they are.
     */
    @Test
    void testAddArgumentEndingInBackslashWithoutSpaceIsNotQuoted() {
        final CommandLine cmdl = new CommandLine("test");
        cmdl.addArgument("C:\\dir\\");
        assertArrayEquals(new String[] { "test", "C:\\dir\\" }, cmdl.toStrings());
    }

    /**
     * An argument without a backslash in front of the closing quote keeps working.
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testAddArgumentWithSpaceIsDeliveredUnchanged() throws Exception {
        assertEquals(Arrays.asList("C:\\Program Files"), runChildProcess("C:\\Program Files"));
        assertEquals(Arrays.asList("key=a b"), runChildProcess("key=a b"));
    }

    @Test
    void testAddArgument() {
        final CommandLine cmdl = new CommandLine("test");
        cmdl.addArgument("foo");
        cmdl.addArgument("bar");
        assertEquals("[test, foo, bar]", cmdl.toString());
        assertArrayEquals(new String[] { "test", "foo", "bar" }, cmdl.toStrings());
    }

    @Test
    void testAddArguments() {
        final CommandLine cmdl = new CommandLine("test");
        cmdl.addArguments("foo bar");
        assertEquals("[test, foo, bar]", cmdl.toString());
        assertArrayEquals(new String[] { "test", "foo", "bar" }, cmdl.toStrings());
    }

    @Test
    void testAddArgumentsArray() {
        final CommandLine cmdl = new CommandLine("test");
        cmdl.addArguments(new String[] { "foo", "bar" });
        assertEquals("[test, foo, bar]", cmdl.toString());
        assertArrayEquals(new String[] { "test", "foo", "bar" }, cmdl.toStrings());
    }

    @Test
    void testAddArgumentsArrayNull() {
        final CommandLine cmdl = new CommandLine("test");
        cmdl.addArguments((String[]) null);
        assertEquals("[test]", cmdl.toString());
        assertArrayEquals(new String[] { "test" }, cmdl.toStrings());
    }

    @Test
    void testAddArgumentsWithQuotes() {
        final CommandLine cmdl = new CommandLine("test");
        cmdl.addArguments("'foo' \"bar\"");
        assertEquals("[test, foo, bar]", cmdl.toString());
        assertArrayEquals(new String[] { "test", "foo", "bar" }, cmdl.toStrings());
    }

    @Test
    void testAddArgumentsWithQuotesAndSpaces() {
        final CommandLine cmdl = new CommandLine("test");
        cmdl.addArguments("'fo o' \"ba r\"");
        assertEquals("[test, \"fo o\", \"ba r\"]", cmdl.toString());
        assertArrayEquals(new String[] { "test", "\"fo o\"", "\"ba r\"" }, cmdl.toStrings());
    }

    @Test
    void testAddArgumentWithBothQuotes() {
        final CommandLine cmdl = new CommandLine("test");
        assertThrows(IllegalArgumentException.class, () -> cmdl.addArgument("b\"a'r"));
    }

    @Test
    void testAddArgumentWithQuote() {
        final CommandLine cmdl = new CommandLine("test");
        cmdl.addArgument("foo");
        cmdl.addArgument("ba\"r");
        assertEquals("[test, foo, 'ba\"r']", cmdl.toString());
        assertArrayEquals(new String[] { "test", "foo", "'ba\"r'" }, cmdl.toStrings());
    }

    @Test
    void testAddArgumentWithQuotesAround() {
        final CommandLine cmdl = new CommandLine("test");
        cmdl.addArgument("\'foo\'");
        cmdl.addArgument("\"bar\"");
        cmdl.addArgument("\"fe z\"");
        assertEquals("[test, foo, bar, \"fe z\"]", cmdl.toString());
        assertArrayEquals(new String[] { "test", "foo", "bar", "\"fe z\"" }, cmdl.toStrings());
    }

    @Test
    void testAddArgumentWithSingleQuote() {
        final CommandLine cmdl = new CommandLine("test");

        cmdl.addArgument("foo");
        cmdl.addArgument("ba'r");
        assertEquals("[test, foo, \"ba'r\"]", cmdl.toString());
        assertArrayEquals(new String[] { "test", "foo", "\"ba\'r\"" }, cmdl.toStrings());
    }

    @Test
    void testAddArgumentWithSpace() {
        final CommandLine cmdl = new CommandLine("test");
        cmdl.addArgument("foo");
        cmdl.addArgument("ba r");
        assertEquals("[test, foo, \"ba r\"]", cmdl.toString());
        assertArrayEquals(new String[] { "test", "foo", "\"ba r\"" }, cmdl.toStrings());
    }

    @Test
    void testAddNullArgument() {
        final CommandLine cmdl = new CommandLine("test");

        cmdl.addArgument(null);
        assertEquals("[test]", cmdl.toString());
        assertArrayEquals(new String[] { "test" }, cmdl.toStrings());
    }

    /**
     * A little example how to add two command line arguments in one line, e.g. to make commenting out some options less error-prone.
     */
    @Test
    void testAddTwoArguments() {

        final CommandLine userAddCL1 = new CommandLine("useradd");
        userAddCL1.addArgument("-g");
        userAddCL1.addArgument("tomcat");
        userAddCL1.addArgument("foo");

        final CommandLine userAddCL2 = new CommandLine("useradd");
        userAddCL2.addArgument("-g").addArgument("tomcat");
        userAddCL2.addArgument("foo");

        assertEquals(userAddCL1.toString(), userAddCL2.toString());
    }

    /**
     * Test expanding the command line based on a user-supplied map.
     */
    @Test
    void testCommandLineParsingWithExpansion1() {

        CommandLine cmdl;

        final Map<String, Object> substitutionMap = new HashMap<>();
        substitutionMap.put("JAVA_HOME", "/usr/local/java");
        substitutionMap.put("appMainClass", "foo.bar.Main");
        substitutionMap.put("file1", new File("./pom.xml"));
        substitutionMap.put("file2", new File(".\\temp\\READ ME.txt"));

        final Map<String, String> incompleteMap = new HashMap<>();
        incompleteMap.put("JAVA_HOME", "/usr/local/java");

        // do not pass substitution map
        cmdl = CommandLine.parse("${JAVA_HOME}/bin/java ${appMainClass}");
        assertTrue(cmdl.getExecutable().startsWith("${JAVA_HOME}"));
        assertArrayEquals(new String[] { "${appMainClass}" }, cmdl.getArguments());

        // pass arguments with an empty map
        cmdl = CommandLine.parse("${JAVA_HOME}/bin/java ${appMainClass}", new HashMap<>());
        assertTrue(cmdl.getExecutable().startsWith("${JAVA_HOME}"));
        assertArrayEquals(new String[] { "${appMainClass}" }, cmdl.getArguments());

        // pass a complete substitution map
        cmdl = CommandLine.parse("${JAVA_HOME}/bin/java ${appMainClass}", substitutionMap);
        assertFalse(cmdl.getExecutable().contains("${JAVA_HOME}"));
        assertTrue(cmdl.getExecutable().indexOf("local") > 0);
        assertArrayEquals(new String[] { "foo.bar.Main" }, cmdl.getArguments());

        // pass an incomplete substitution map resulting in unresolved variables
        cmdl = CommandLine.parse("${JAVA_HOME}/bin/java ${appMainClass}", incompleteMap);
        assertFalse(cmdl.getExecutable().contains("${JAVA_HOME}"));
        assertTrue(cmdl.getExecutable().indexOf("local") > 0);
        assertArrayEquals(new String[] { "${appMainClass}" }, cmdl.getArguments());

        // pass a file
        cmdl = CommandLine.parse("${JAVA_HOME}/bin/java ${appMainClass} ${file1} ${file2}", substitutionMap);
        assertFalse(cmdl.getExecutable().contains("${file}"));
    }

    /**
     * Test expanding the command line based on a user-supplied map. The main goal of the test is to set up a command line using macros and reuse it multiple
     * times.
     */
    @Test
    void testCommandLineParsingWithExpansion2() {

        CommandLine cmdl;
        String[] result;

        // build the user supplied parameters
        final Map<String, String> substitutionMap = new HashMap<>();
        substitutionMap.put("JAVA_HOME", "C:\\Programme\\jdk1.5.0_12");
        substitutionMap.put("appMainClass", "foo.bar.Main");

        // build the command line
        cmdl = new CommandLine("${JAVA_HOME}\\bin\\java");
        cmdl.addArgument("-class");
        cmdl.addArgument("${appMainClass}");
        cmdl.addArgument("${file}");

        // build the first command line
        substitutionMap.put("file", "C:\\Document And Settings\\documents\\432431.pdf");
        cmdl.setSubstitutionMap(substitutionMap);
        result = cmdl.toStrings();

        // verify the first command line
        // please note - the executable argument is changed to using platform specific file separator char
        // whereas all other variable substitution are not touched
        assertEquals(StringUtils.fixFileSeparatorChar("C:\\Programme\\jdk1.5.0_12\\bin\\java"), result[0]);
        assertEquals("-class", result[1]);
        assertEquals("foo.bar.Main", result[2]);
        assertEquals("\"C:\\Document And Settings\\documents\\432431.pdf\"", result[3]);

        // verify the first command line again but by
        // accessing the executable and arguments directly
        final String executable = cmdl.getExecutable();
        final String[] arguments = cmdl.getArguments();
        assertEquals(StringUtils.fixFileSeparatorChar("C:\\Programme\\jdk1.5.0_12\\bin\\java"), executable);
        assertEquals("-class", arguments[0]);
        assertEquals("foo.bar.Main", arguments[1]);
        assertEquals("\"C:\\Document And Settings\\documents\\432431.pdf\"", arguments[2]);

        // build the second command line with updated parameters resulting in a different command line
        substitutionMap.put("file", "C:\\Document And Settings\\documents\\432432.pdf");
        result = cmdl.toStrings();
        assertEquals(StringUtils.fixFileSeparatorChar("C:\\Programme\\jdk1.5.0_12\\bin\\java"), result[0]);
        assertEquals("-class", result[1]);
        assertEquals("foo.bar.Main", result[2]);
        assertEquals("\"C:\\Document And Settings\\documents\\432432.pdf\"", result[3]);
    }

    @Test
    void testCommandLineParsingWithExpansion3() {
        final CommandLine cmdl = CommandLine.parse("AcroRd32.exe");
        cmdl.addArgument("/p");
        cmdl.addArgument("/h");
        cmdl.addArgument("${file}", false);
        final Map<String, String> params = new HashMap<>();
        params.put("file", "C:\\Document And Settings\\documents\\432432.pdf");
        cmdl.setSubstitutionMap(params);
        final String[] result = cmdl.toStrings();
        assertEquals("AcroRd32.exe", result[0]);
        assertEquals("/p", result[1]);
        assertEquals("/h", result[2]);
        assertEquals("C:\\Document And Settings\\documents\\432432.pdf", result[3]);

    }

    /**
     * Create a command line with pre-quoted strings to test SANDBOX-192, e.g. "runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC",
     * "\"-XX:ParallelGCThreads=2\""
     */
    @Test
    void testComplexAddArgument() {
        final CommandLine cmdl = new CommandLine("runMemorySud.cmd");
        cmdl.addArgument("10", false);
        cmdl.addArgument("30", false);
        cmdl.addArgument("-XX:+UseParallelGC", false);
        cmdl.addArgument("\"-XX:ParallelGCThreads=2\"", false);
        assertArrayEquals(new String[] { "runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\"" }, cmdl.toStrings());
    }

    /**
     * Create a command line with pre-quoted strings to test SANDBOX-192, e.g. "runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC",
     * "\"-XX:ParallelGCThreads=2\""
     */
    @Test
    void testComplexAddArguments1() {
        final CommandLine cmdl = new CommandLine("runMemorySud.cmd");
        cmdl.addArguments(new String[] { "10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\"" }, false);
        assertArrayEquals(new String[] { "runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\"" }, cmdl.toStrings());
    }

    /**
     * Create a command line with pre-quoted strings to test SANDBOX-192, e.g. "runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC",
     * "\"-XX:ParallelGCThreads=2\"" Please note that we re forced to add additional single quotes to get the test working - don't know if this is a bug or a
     * feature.
     */
    @Test
    void testComplexAddArguments2() {
        final CommandLine cmdl = new CommandLine("runMemorySud.cmd");
        cmdl.addArguments("10 30 -XX:+UseParallelGC '\"-XX:ParallelGCThreads=2\"'", false);
        assertArrayEquals(new String[] { "runMemorySud.cmd", "10", "30", "-XX:+UseParallelGC", "\"-XX:ParallelGCThreads=2\"" }, cmdl.toStrings());
    }

    @Test
    void testCopyConstructor() {
        final Map<String, String> map = new HashMap<>();
        map.put("bar", "bar");
        final CommandLine other = new CommandLine("test");
        other.addArgument("foo");
        other.setSubstitutionMap(map);

        final CommandLine cmdl = new CommandLine(other);
        assertEquals(other.getExecutable(), cmdl.getExecutable());
        assertArrayEquals(other.getArguments(), cmdl.getArguments());
        assertEquals(other.isFile(), cmdl.isFile());
        assertEquals(other.getSubstitutionMap(), cmdl.getSubstitutionMap());

    }

    @Test
    void testExecutable() {
        final CommandLine cmdl = new CommandLine("test");
        assertEquals("[test]", cmdl.toString());
        assertArrayEquals(new String[] { "test" }, cmdl.toStrings());
        assertEquals("test", cmdl.getExecutable());
        assertTrue(cmdl.getArguments().length == 0);
    }

    @Test
    void testExecutableWhitespaceString() {
        assertThrows(IllegalArgumentException.class, () -> new CommandLine("   "));
    }

    @Test
    void testExecutableZeroLengthString() {
        assertThrows(IllegalArgumentException.class, () -> new CommandLine(""));
    }

    @Test
    void testNullExecutable() {
        assertThrows(NullPointerException.class, () -> new CommandLine((String) null));
    }

    @Test
    void testParseCommandLine() {
        final CommandLine cmdl = CommandLine.parse("test foo bar");
        assertEquals("[test, foo, bar]", cmdl.toString());
        assertArrayEquals(new String[] { "test", "foo", "bar" }, cmdl.toStrings());
    }

    @Test
    void testParseCommandLineWithNull() {
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse(null));
    }

    @Test
    void testParseCommandLineWithOnlyWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse("  "));
    }

    @Test
    void testParseCommandLineWithQuotes() {
        final CommandLine cmdl = CommandLine.parse("test \"foo\" \'ba r\'");
        assertEquals("[test, foo, \"ba r\"]", cmdl.toString());
        assertArrayEquals(new String[] { "test", "foo", "\"ba r\"" }, cmdl.toStrings());
    }

    @Test
    void testParseCommandLineWithUnevenQuotes() {
        assertThrows(IllegalArgumentException.class, () -> CommandLine.parse("test \"foo bar"), "IllegalArgumentException must be thrown due to uneven quotes");
    }

    /**
     * A command line parsing puzzle from Tino Schoellhorn - ImageMagix expects a "500x>" parameter (including quotes) and it is simply not possible to do that
     * without adding a space, e.g. "500x> ".
     */
    @Test
    void testParseComplexCommandLine1() {
        final Map<String, String> substitutionMap = new HashMap<>();
        substitutionMap.put("in", "source.jpg");
        substitutionMap.put("out", "target.jpg");
        final CommandLine cmdl = CommandLine.parse("cmd /C convert ${in} -resize \"\'500x> \'\" ${out}", substitutionMap);
        assertEquals("[cmd, /C, convert, source.jpg, -resize, \"500x> \", target.jpg]", cmdl.toString());
    }

    /**
     * Another command line parsing puzzle from Kai Hu - as far as I understand it there is no way to express that in a one-line command string.
     */
    @Test
    void testParseComplexCommandLine2() {
        final String commandLine = "./script/jrake cruise:publish_installers INSTALLER_VERSION=unstable_2_1 "
                + "INSTALLER_PATH=\"/var/lib/ cruise-agent/installers\" INSTALLER_DOWNLOAD_SERVER=\'something\' WITHOUT_HELP_DOC=true";
        final CommandLine cmdl = CommandLine.parse(commandLine);
        final String[] args = cmdl.getArguments();
        assertEquals(args[0], "cruise:publish_installers");
        assertEquals(args[1], "INSTALLER_VERSION=unstable_2_1");
        // assertEquals(args[2], "INSTALLER_PATH=\"/var/lib/ cruise-agent/installers\"");
        // assertEquals(args[3], "INSTALLER_DOWNLOAD_SERVER='something'");
        assertEquals(args[4], "WITHOUT_HELP_DOC=true");
    }

    /**
     * Test the following command line:
     * <pre>
     * cmd.exe /C c:\was51\Web Sphere\AppServer\bin\versionInfo.bat
     * </pre>
     */
    @Test
    void testParseRealLifeCommandLine() {

        final String commandLine = "cmd.exe /C \"c:\\was51\\Web Sphere\\AppServer\\bin\\versionInfo.bat\"";

        final CommandLine cmdl = CommandLine.parse(commandLine);
        final String[] args = cmdl.getArguments();
        assertEquals("/C", args[0]);
        assertEquals("\"c:\\was51\\Web Sphere\\AppServer\\bin\\versionInfo.bat\"", args[1]);
    }

    /**
     * Test the toString() method.
     *
     * @throws Exception the test failed
     */
    @Test
    void testToString() throws Exception {
        CommandLine cmdl;
        final Map<String, String> params = new HashMap<>();

        // use no arguments
        cmdl = CommandLine.parse("AcroRd32.exe", params);
        assertEquals("[AcroRd32.exe]", cmdl.toString());

        // use an argument containing spaces
        params.put("file", "C:\\Document And Settings\\documents\\432432.pdf");
        cmdl = CommandLine.parse("AcroRd32.exe /p /h '${file}'", params);
        assertEquals("[AcroRd32.exe, /p, /h, \"C:\\Document And Settings\\documents\\432432.pdf\"]", cmdl.toString());

        // use an argument without spaces
        params.put("file", "C:\\documents\\432432.pdf");
        cmdl = CommandLine.parse("AcroRd32.exe /p /h '${file}'", params);
        assertEquals("[AcroRd32.exe, /p, /h, C:\\documents\\432432.pdf]", cmdl.toString());
    }

    /**
     * Test that toString() produces output that is useful for troubleshooting.
     *
     * @throws Exception the test failed
     */
    @Test
    void testToStringTroubleshooting() throws Exception {
        // On HP-UX quotes handling leads to errors,
        // also usage of quotes isn't mandatory on other platforms too
        // so it probably should work correctly either way.
        final CommandLine cmd1 = new CommandLine("sh").addArgument("-c").addArgument("echo 1", false);
        final CommandLine cmd2 = new CommandLine("sh").addArgument("-c").addArgument("echo").addArgument("1");
        assertTrue(!cmd1.toString().equals(cmd2.toString()), "toString() is useful for troubleshooting");
    }

}
