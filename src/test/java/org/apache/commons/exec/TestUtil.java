package org.apache.commons.exec;

import junit.framework.AssertionFailedError;

public final class TestUtil {

    private TestUtil() {

    }

    public static String resolveScriptForOS(String script) {
        if (OS.isFamilyWindows()) {
            return script + ".bat";
        } else if (OS.isFamilyUnix()) {
            return script + ".sh";
        } else {
            throw new AssertionFailedError("Test not supported for this OS");
        }
    }
}
