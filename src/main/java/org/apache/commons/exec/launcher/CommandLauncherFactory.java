package org.apache.commons.exec.launcher;

import org.apache.commons.exec.OS;

/**
 * Builds a command launcher for the OS and JVM we are running under.
 */

public final class CommandLauncherFactory {

    private CommandLauncherFactory() {

    }

    /**
     * 
     */
    public static CommandLauncher createVMLauncher() {
        // Try using a JDK 1.3 launcher
        CommandLauncher launcher = null;
        try {
            if (OS.isFamilyOpenVms()) {
                launcher = new VmsCommandLauncher();
            } else if (!OS.isFamilyOS2()) {
                launcher = new Java13CommandLauncher();
            }
        } catch (NoSuchMethodException exc) {
            // Ignore and keep trying
        }

        return launcher;
    }

    public static CommandLauncher createShellLauncher() {
        CommandLauncher launcher = null;

        if (OS.isFamilyMac() && !OS.isFamilyUnix()) {
            // Mac
            launcher = new MacCommandLauncher(new CommandLauncherImpl());
        } else if (OS.isFamilyOS2()) {
            // OS/2
            launcher = new OS2CommandLauncher(new CommandLauncherImpl());
        } else if (OS.isFamilyWindows()) {
            // Windows. Need to determine which JDK we're running in

            CommandLauncher baseLauncher;
            if (System.getProperty("java.version").startsWith("1.1")) {
                // JDK 1.1
                baseLauncher = new Java11CommandLauncher();
            } else {
                // JDK 1.2
                baseLauncher = new CommandLauncherImpl();
            }

            if (!OS.isFamilyWin9x()) {
                // Windows XP/2000/NT
                launcher = new WinNTCommandLauncher(baseLauncher);
            } else {
                // Windows 98/95 - need to use an auxiliary script
                launcher = new ScriptCommandLauncher("bin/antRun.bat",
                        baseLauncher);
            }
        } else if (OS.isFamilyNetware()) {
            // NetWare. Need to determine which JDK we're running in
            CommandLauncher baseLauncher;
            if (System.getProperty("java.version").startsWith("1.1")) {
                // JDK 1.1
                baseLauncher = new Java11CommandLauncher();
            } else {
                // JDK 1.2
                baseLauncher = new CommandLauncherImpl();
            }

            launcher = new PerlScriptCommandLauncher("bin/antRun.pl",
                    baseLauncher);
        } else if (OS.isFamilyOpenVms()) {
            // the vmLauncher already uses the shell
            launcher = createVMLauncher();
        } else {
            // Generic
            launcher = new ScriptCommandLauncher("bin/antRun",
                    new CommandLauncherImpl());
        }

        return launcher;
    }
}
