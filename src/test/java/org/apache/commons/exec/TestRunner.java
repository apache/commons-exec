package org.apache.commons.exec;

import junit.framework.*;

import org.apache.commons.exec.environment.EnvironmentUtilTest;
import org.apache.commons.exec.util.MapUtilTest;

public class TestRunner extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite("TestRunner");
        suite.addTestSuite(CommandLineTest.class);
        suite.addTestSuite(DefaultExecutorTest.class);
        suite.addTestSuite(EnvironmentUtilTest.class);
        suite.addTestSuite(MapUtilTest.class);
        suite.addTestSuite(TestUtilTest.class);
        return suite;
    }

    public static void main(String[] args) {
        Test test = TestRunner.suite();
        junit.textui.TestRunner testRunner = new junit.textui.TestRunner(System.out);
        TestResult testResult = testRunner.doRun(test);
        return;
    }
}
