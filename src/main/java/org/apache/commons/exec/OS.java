/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.exec;

import java.io.File;
import java.util.Locale;

/**
 * Condition that tests the OS type.
 *
 * Copied and adapted from Apache Ant 1.9.6 from org.apache.tools.ant.taskdefs.condition.OS.
 */
public final class OS {

    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_9X = "win9x";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_DOS = "dos";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_MAC = "mac";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_NETWARE = "netware";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_NT = "winnt";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_OS2 = "os/2";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_OS400 = "os/400";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_TANDEM = "tandem";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_UNIX = "unix";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_VMS = "openvms";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_WINDOWS = "windows";
    /**
     * OS family that can be tested for. {@value}
     */
    public static final String FAMILY_ZOS = "z/os";

    private static final String DARWIN = "darwin";

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
    private static final String OS_VERSION = System.getProperty("os.version").toLowerCase(Locale.ENGLISH);
    private static final String PATH_SEP = File.pathSeparator;

    /**
     * Tests whether the OS on which commons-exec is executing matches the given OS architecture.
     *
     * @param arch the OS architecture to check for.
     * @return whether if the OS matches.
     */
    public static boolean isArch(final String arch) {
        return isOs(null, null, arch, null);
    }

    /**
     * Tests whether the OS on which commons-exec is executing matches the given OS family.
     *
     * @param family the family to check for.
     * @return whether if the OS matches.
     */
    private static boolean isFamily(final String family) {
        return isOs(family, null, null, null);
    }

    /**
     * Tests whether the OS is in the DOS family.
     *
     * @return whether the OS is in the DOS family.
     */
    public static boolean isFamilyDOS() {
        return isFamily(FAMILY_DOS);
    }

    /**
     * Tests whether the OS is in the Mac family.
     *
     * @return whether the OS is in the Mac family.
     */
    public static boolean isFamilyMac() {
        return isFamily(FAMILY_MAC);
    }

    /**
     * Tests whether the OS is in the Netware family.
     *
     * @return whether the OS is in the Netware family.
     */
    public static boolean isFamilyNetware() {
        return isFamily(FAMILY_NETWARE);
    }

    /**
     * Tests whether the OS is in the OpenVMS family.
     *
     * @return whether the OS is in the OpenVMS family.
     */
    public static boolean isFamilyOpenVms() {
        return isFamily(FAMILY_VMS);
    }

    /**
     * Tests whether the OS is in the OS/2 family.
     *
     * @return whether the OS is in the OS/2 family.
     */
    public static boolean isFamilyOS2() {
        return isFamily(FAMILY_OS2);
    }

    /**
     * Tests whether the OS is in the OS/400 family.
     *
     * @return whether the OS is in the OS/400 family.
     */
    public static boolean isFamilyOS400() {
        return isFamily(FAMILY_OS400);
    }

    /**
     * Tests whether the OS is in the Tandem family.
     *
     * @return whether the OS is in the Tandem family.
     */
    public static boolean isFamilyTandem() {
        return isFamily(FAMILY_TANDEM);
    }

    /**
     * Tests whether the OS is in the Unix family.
     *
     * @return whether the OS is in the Unix family.
     */
    public static boolean isFamilyUnix() {
        return isFamily(FAMILY_UNIX);
    }

    /**
     * Tests whether the OS is in the Windows 9x family.
     *
     * @return whether the OS is in the Windows 9x family.
     */
    public static boolean isFamilyWin9x() {
        return isFamily(FAMILY_9X);
    }

    /**
     * Tests whether the OS is in the Windows family.
     *
     * @return whether the OS is in the Windows family.
     */
    public static boolean isFamilyWindows() {
        return isFamily(FAMILY_WINDOWS);
    }

    /**
     * Tests whether the OS is in the Windows NT family.
     *
     * @return whether the OS is in the Windows NT family.
     */
    public static boolean isFamilyWinNT() {
        return isFamily(FAMILY_NT);
    }

    /**
     * Tests whether the OS is in the z/OS family.
     *
     * @return whether the OS is in the z/OS family.
     */
    public static boolean isFamilyZOS() {
        return isFamily(FAMILY_ZOS);
    }

    /**
     * Tests whether if the OS on which commons-exec is executing matches the given OS name.
     *
     * @param name the OS name to check for.
     * @return whether the OS matches.
     */
    public static boolean isName(final String name) {
        return isOs(null, name, null, null);
    }

    /**
     * Tests whether the OS on which commons-exec is executing matches the given OS family, name, architecture and version.
     *
     * @param family  The OS family.
     * @param name    The OS name.
     * @param arch    The OS architecture.
     * @param version The OS version.
     * @return whether the OS matches.
     */
    public static boolean isOs(final String family, final String name, final String arch, final String version) {
        boolean retValue = false;
        if (family != null || name != null || arch != null || version != null) {
            boolean isFamily = true;
            boolean isName = true;
            boolean isArch = true;
            boolean isVersion = true;
            if (family != null) {
                // Windows probing logic relies on the word 'windows' in the OS
                final boolean isWindows = OS_NAME.contains(FAMILY_WINDOWS);
                boolean is9x = false;
                boolean isNT = false;
                if (isWindows) {
                    // there are only four 9x platforms that we look for
                    is9x = OS_NAME.contains("95") || OS_NAME.contains("98") || OS_NAME.contains("me")
                    // Windows CE isn't really 9x, but crippled enough to
                    // be a muchness. Ant doesn't run on CE, anyway.
                            || OS_NAME.contains("ce");
                    isNT = !is9x;
                }
                switch (family) {
                case FAMILY_WINDOWS:
                    isFamily = isWindows;
                    break;
                case FAMILY_9X:
                    isFamily = isWindows && is9x;
                    break;
                case FAMILY_NT:
                    isFamily = isWindows && isNT;
                    break;
                case FAMILY_OS2:
                    isFamily = OS_NAME.contains(FAMILY_OS2);
                    break;
                case FAMILY_NETWARE:
                    isFamily = OS_NAME.contains(FAMILY_NETWARE);
                    break;
                case FAMILY_DOS:
                    isFamily = PATH_SEP.equals(";") && !isFamily(FAMILY_NETWARE);
                    break;
                case FAMILY_MAC:
                    isFamily = OS_NAME.contains(FAMILY_MAC) || OS_NAME.contains(DARWIN);
                    break;
                case FAMILY_TANDEM:
                    isFamily = OS_NAME.contains("nonstop_kernel");
                    break;
                case FAMILY_UNIX:
                    isFamily = PATH_SEP.equals(":") && !isFamily(FAMILY_VMS) && (!isFamily(FAMILY_MAC) || OS_NAME.endsWith("x") || OS_NAME.contains(DARWIN));
                    break;
                case FAMILY_ZOS:
                    isFamily = OS_NAME.contains(FAMILY_ZOS) || OS_NAME.contains("os/390");
                    break;
                case FAMILY_OS400:
                    isFamily = OS_NAME.contains(FAMILY_OS400);
                    break;
                case FAMILY_VMS:
                    isFamily = OS_NAME.contains(FAMILY_VMS);
                    break;
                default:
                    throw new IllegalArgumentException("Don\'t know how to detect OS family \"" + family + "\"");
                }
            }
            if (name != null) {
                isName = name.equals(OS_NAME);
            }
            if (arch != null) {
                isArch = arch.equals(OS_ARCH);
            }
            if (version != null) {
                isVersion = version.equals(OS_VERSION);
            }
            retValue = isFamily && isName && isArch && isVersion;
        }
        return retValue;
    }

    /**
     * Tests whether the OS on which commonss-exec is executing matches the given OS version.
     *
     * @param version the OS version to check for.
     * @return whether if the OS matches.
     */
    public static boolean isVersion(final String version) {
        return isOs(null, null, null, version);
    }

    /**
     * Avoids instances.
     */
    private OS() {
    }
}
