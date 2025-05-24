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

package org.apache.commons.exec.launcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;

public class VmsCommandLauncherTest extends AbstractCommandLauncherTest<VmsCommandLauncher> {

    @Override
    VmsCommandLauncher createCommandLauncher() {
        return new VmsCommandLauncher();
    }

    @Test
    public void testCreateCommandFile() throws IOException {
        final VmsCommandLauncher commandLauncher = createCommandLauncher();
        final CommandLine cl = CommandLine.parse("a b \"c d\"");
        assertNotNull(commandLauncher.createCommandFile(cl, null));
        final HashMap<String, String> env = new HashMap<>();
        assertNotNull(commandLauncher.createCommandFile(cl, env));
        env.put("EnvKey", "EnvValue");
        assertNotNull(commandLauncher.createCommandFile(cl, env));

    }

    @Override
    @Test
    public void testIsFailure() throws Exception {
        final CommandLauncher commandLauncher = createCommandLauncher();
        assertTrue(commandLauncher.isFailure(2));
        assertFalse(commandLauncher.isFailure(1));
    }

    @Override
    @Test
    public void testIsFailureZero() throws Exception {
        assertTrue(createCommandLauncher().isFailure(0));
    }

}
