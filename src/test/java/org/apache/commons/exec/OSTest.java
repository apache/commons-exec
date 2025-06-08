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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

/**
 * Tests {@link OS}.
 */
class OSTest {

    @Test
    void testIsArch() {
        assertFalse(OS.isArch(null));
        assertFalse(OS.isArch("...."));
    }

    @Test
    @EnabledOnOs(org.junit.jupiter.api.condition.OS.MAC)
    void testIsArchMacOs() {
        assertFalse(OS.isFamilyDOS());
        assertTrue(OS.isFamilyMac());
        assertFalse(OS.isFamilyNetware());
        assertFalse(OS.isFamilyOpenVms());
        assertFalse(OS.isFamilyOS2());
        assertFalse(OS.isFamilyOS400());
        assertFalse(OS.isFamilyTandem());
        assertTrue(OS.isFamilyUnix());
        assertFalse(OS.isFamilyWin9x());
        assertFalse(OS.isFamilyWindows());
        assertFalse(OS.isFamilyWinNT());
        assertFalse(OS.isFamilyZOS());
    }

    @Test
    void testIsVersion() {
        assertFalse(OS.isVersion(null));
        assertFalse(OS.isVersion("...."));
    }
}
