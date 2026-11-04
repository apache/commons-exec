/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.exec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Child process used by {@code CommandLineTest} to report the arguments the operating system handed to it. Each argument is stored in a separate file so that
 * the parent can tell an argument that was dropped from an argument that was split in two, and so that the console encoding cannot alter the argument.
 */
public final class QuotedArgumentRecorder {

    /**
     * Writes every argument to a numbered file inside the directory given as the first argument.
     *
     * @param args {@code args[0]} is the output directory, the remaining values are the arguments to record.
     * @throws IOException if the files cannot be written.
     */
    public static void main(final String[] args) throws IOException {
        final Path dir = Paths.get(args[0]);
        Files.createDirectories(dir);
        for (int i = 1; i < args.length; i++) {
            Files.write(dir.resolve(String.format("%03d.txt", i - 1)), args[i].getBytes(StandardCharsets.UTF_8));
        }
        Files.write(dir.resolve("count.txt"), String.valueOf(args.length - 1).getBytes(StandardCharsets.UTF_8));
    }

    private QuotedArgumentRecorder() {
    }
}
