/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.exec.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test EXEC-36 see https://issues.apache.org/jira/browse/EXEC-36
 *
 * @version $Id$
 */
public class Exec36Test {


    private final Executor exec = new DefaultExecutor();
    private final File testDir = new File("src/test/scripts");
    private final File printArgsScript = TestUtil.resolveScriptForOS(testDir + "/printargs");

    private ByteArrayOutputStream baos;

    @Before
    public void setUp() throws Exception {
        // prepare a ready to Executor
        this.baos = new ByteArrayOutputStream();
        this.exec.setStreamHandler(new PumpStreamHandler(baos, baos));
    }

    @After
    public void tearDown() throws Exception {
        this.baos.close();
    }

    /**
     *
     * Original example from Kai Hu which only can be tested on Unix
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec36_1() throws Exception {

        if (OS.isFamilyUnix()) {

            CommandLine cmdl;

            /**
             * ./script/jrake cruise:publish_installers INSTALLER_VERSION=unstable_2_1 \
             *     INSTALLER_PATH="/var/lib/ cruise-agent/installers" INSTALLER_DOWNLOAD_SERVER='something' WITHOUT_HELP_DOC=true
             */

            final String expected = "./script/jrake\n" +
                    "cruise:publish_installers\n" +
                    "INSTALLER_VERSION=unstable_2_1\n" +
                    "INSTALLER_PATH=\"/var/lib/ cruise-agent/installers\"\n" +
                    "INSTALLER_DOWNLOAD_SERVER='something'\n" +
                    "WITHOUT_HELP_DOC=true";

            cmdl = new CommandLine(printArgsScript);
            cmdl.addArgument("./script/jrake", false);
            cmdl.addArgument("cruise:publish_installers", false);
            cmdl.addArgument("INSTALLER_VERSION=unstable_2_1", false);
            cmdl.addArgument("INSTALLER_PATH=\"/var/lib/ cruise-agent/installers\"", false);
            cmdl.addArgument("INSTALLER_DOWNLOAD_SERVER='something'", false);
            cmdl.addArgument("WITHOUT_HELP_DOC=true", false);

            final int exitValue = exec.execute(cmdl);
            final String result = baos.toString().trim();
            assertFalse(exec.isFailure(exitValue));
            assertEquals(expected, result);
        }
        else {
            System.err.println("The test 'testExec36_1' does not support the following OS : " + System.getProperty("os.name"));
            return;
        }
    }

    /**
     * Test a complex real example found at
     * http://blogs.msdn.com/b/astebner/archive/2005/12/13/503471.aspx
     *
     * The command line is so weird that it even falls apart under Windows
     *
     * @throws Exception the test failed
     */
    @Test
    public void testExec36_2() throws Exception {

        String expected;

        // the original command line
        // dotnetfx.exe /q:a /c:"install.exe /l ""\Documents and Settings\myusername\Local Settings\Temp\netfx.log"" /q"

        if (OS.isFamilyWindows()) {
            expected = "dotnetfx.exe\n" +
                    "/q:a\n" +
                    "/c:\"install.exe /l \"\"\\Documents and Settings\\myusername\\Local Settings\\Temp\\netfx.log\"\" /q\"";
        }
        else if (OS.isFamilyUnix()) {
            expected = "dotnetfx.exe\n" +
                    "/q:a\n" +
                    "/c:\"install.exe /l \"\"/Documents and Settings/myusername/Local Settings/Temp/netfx.log\"\" /q\"";
        }
        else {
            System.err.println("The test 'testExec36_3' does not support the following OS : " + System.getProperty("os.name"));
            return;
        }

        CommandLine cmdl;
        final File file = new File("/Documents and Settings/myusername/Local Settings/Temp/netfx.log");
        final Map<String, File> map = new HashMap<String, File>();
        map.put("FILE", file);

        cmdl = new CommandLine(printArgsScript);
        cmdl.setSubstitutionMap(map);
        cmdl.addArgument("dotnetfx.exe", false);
        cmdl.addArgument("/q:a", false);
        cmdl.addArgument("/c:\"install.exe /l \"\"${FILE}\"\" /q\"", false);

        final int exitValue = exec.execute(cmdl);
        final String result = baos.toString().trim();
        assertFalse(exec.isFailure(exitValue));

        if (OS.isFamilyUnix()) {
            // the parameters fall literally apart under Windows - need to disable the check for Win32
            assertEquals(expected, result);
        }
    }

    /**
     * Some complex real-life command line from
     * http://blogs.msdn.com/b/astebner/archive/2005/12/13/503471.aspx
     */
    @Test
    @Ignore
    public void _testExec36_4() throws Exception {

        CommandLine cmdl;

        final String line = "./script/jrake "
                + "cruise:publish_installers "
                + "INSTALLER_VERSION=unstable_2_1 "
                + "INSTALLER_PATH=\"/var/lib/cruise-agent/installers\" "
                + "INSTALLER_DOWNLOAD_SERVER='something'"
                + "WITHOUT_HELP_DOC=true";

        cmdl = CommandLine.parse(line);
        final String[] args = cmdl.toStrings();
        assertEquals("./script/jrake", args[0]);
        assertEquals("cruise:publish_installers", args[1]);
        assertEquals("INSTALLER_VERSION=unstable_2_1", args[2]);
        assertEquals("INSTALLER_PATH=\"/var/lib/cruise-agent/installers\"", args[3]);
        assertEquals("INSTALLER_DOWNLOAD_SERVER='something'", args[4]);
        assertEquals("WITHOUT_HELP_DOC=true", args[5]);
    }

    /**
     * Some complex real-life command line from
     * http://blogs.msdn.com/b/astebner/archive/2005/12/13/503471.aspx
     */
    @Test
    @Ignore
    public void _testExec36_5() {

        CommandLine cmdl;

        final String line = "dotnetfx.exe"
                + " /q:a "
                + "/c:\"install.exe /l \"\"c:\\Documents and Settings\\myusername\\Local Settings\\Temp\\netfx.log\"\" /q\"";

        cmdl = CommandLine.parse(line);
        final String[] args = cmdl.toStrings();
        assertEquals("dotnetfx.exe", args[0]);
        assertEquals("/q:a", args[1]);
        assertEquals("/c:\"install.exe /l \"\"c:\\Documents and Settings\\myusername\\Local Settings\\Temp\\netfx.log\"\" /q\"", args[2] );
    }

    /**
     * Test the following command line
     *
     * C:\CVS_DB\WeightsEngine /f WeightsEngine.mak CFG="WeightsEngine - Win32Release"
     */
    @Test
    @Ignore
    public void _testExec36_6() {

        final String commandline = "C:\\CVS_DB\\WeightsEngine /f WeightsEngine.mak CFG=\"WeightsEngine - Win32Release\"";

        final CommandLine cmdl = CommandLine.parse(commandline);
        final String[] args = cmdl.getArguments();
        assertEquals("/f", args[0]);
        assertEquals("WeightsEngine.mak", args[1]);
        assertEquals("CFG=\"WeightsEngine - Win32Release\"", args[2]);
    }
}
