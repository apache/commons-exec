package org.apache.commons.exec;

import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.File;

public abstract class AbstractExecTest {

    public static final int TEST_TIMEOUT = 15000;
    public static final int WATCHDOG_TIMEOUT = 3000;

    private static final String OS_NAME = System.getProperty("os.name");

    private final File testDir = new File("src/test/scripts");

    @Rule public TestName name = new TestName();

    /**
     * Resolve the OS-specific test file to execute.
     */
    protected File resolveTestScript(String baseName) {
        final File result = TestUtil.resolveScriptForOS(testDir + "/" + baseName);
        if (!result.exists()) {
            throw new IllegalArgumentException("Unable to find the following file: " + result.getAbsolutePath());
        }
        return result;
    }

    /**
     * Resolve the OS-specific test file to execute.
     */
    protected File resolveTestScript(String directoryName, String baseName) {
        final File result = TestUtil.resolveScriptForOS(testDir + "/" + directoryName + "/" + baseName);
        if (!result.exists()) {
            throw new IllegalArgumentException("Unable to find the following file: " + result.getAbsolutePath());
        }
        return result;
    }

    protected String getName() {
        return name.getMethodName();
    }

    protected String testNotSupportedForCurrentOperatingSystem() {
        final String msg = String.format("The test '%s' does not support the following OS : %s", name.getMethodName(), OS_NAME);
        System.out.println(msg);
        return msg;
    }
}
