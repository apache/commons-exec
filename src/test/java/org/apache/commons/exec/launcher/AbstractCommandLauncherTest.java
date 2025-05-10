/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.commons.exec.launcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

abstract class AbstractCommandLauncherTest<T extends CommandLauncher> {

    abstract T createCommandLauncher();

    @Test
    public void testIsFailure() throws Exception {
        final T commandLauncher = createCommandLauncher();
        assertTrue(commandLauncher.isFailure(2));
        assertTrue(commandLauncher.isFailure(1));
    }

    @Test
    public void testIsFailureZero() throws Exception {
        assertFalse(createCommandLauncher().isFailure(0));
    }

}
