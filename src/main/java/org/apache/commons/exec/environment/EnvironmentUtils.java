/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.commons.exec.environment;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Wraps environment variables.
 */
public class EnvironmentUtils {

    private static final DefaultProcessingEnvironment ENVIRONMENT;

    static {
        ENVIRONMENT = new DefaultProcessingEnvironment();
    }

    /**
     * Adds a key/value pair to the given environment. If the key matches an existing key, the previous setting is replaced.
     *
     * @param environment the current environment.
     * @param keyAndValue the key/value pair.
     */
    public static void addVariableToEnvironment(final Map<String, String> environment, final String keyAndValue) {
        final String[] parsedVariable = parseEnvironmentVariable(keyAndValue);
        environment.put(parsedVariable[0], parsedVariable[1]);
    }

    /**
     * Gets the list of environment variables for this process. The returned map preserves the casing of a variable's name on all platforms but obeys the casing
     * rules of the current platform during lookup, e.g. key names will be case-insensitive on Windows platforms.
     *
     * @return a map containing the environment variables, may be empty but never {@code null}.
     * @throws IOException the operation failed.
     */
    public static Map<String, String> getProcEnvironment() throws IOException {
        return ENVIRONMENT.getProcEnvironment();
    }

    /**
     * Parses a key/value pair into a String[]. It is assumed that the ky/value pair contains a '=' character.
     *
     * @param keyAndValue the key/value pair.
     * @return a String[] containing the key and value.
     */
    private static String[] parseEnvironmentVariable(final String keyAndValue) {
        final int index = keyAndValue.indexOf('=');
        if (index == -1) {
            throw new IllegalArgumentException("Environment variable for this platform must contain an equals sign ('=')");
        }
        final String[] result = new String[2];
        result[0] = keyAndValue.substring(0, index);
        result[1] = keyAndValue.substring(index + 1);
        return result;
    }

    private static String toString(final String value) {
        return Objects.toString(value, "");
    }

    /**
     * Converts a variable map as an array.
     *
     * @param environment the environment to use, may be {@code null}.
     * @return array of key=value assignment strings or {@code null} if and only if the input map was {@code null}.
     */
    public static String[] toStrings(final Map<String, String> environment) {
        if (environment == null) {
            return null;
        }
        return environment.entrySet().stream().map(e -> toString(e.getKey()) + "=" + toString(e.getValue())).toArray(String[]::new);
    }

    /**
     * Hides constructor.
     */
    private EnvironmentUtils() {
        // empty
    }

}
