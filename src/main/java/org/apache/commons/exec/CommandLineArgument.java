/* 
 * Copyright 2005  The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.exec;

import java.io.File;

/**
 * Used for nested xml command line definitions.
 */
public class CommandLineArgument {

    private String[] parts;

    public CommandLineArgument(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("value can not be null");
        }

        parts = new String[] {value};
    }

    public CommandLineArgument(final String[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values can not be null");
        }

        parts = values;
    }

    public CommandLineArgument(final File file) {
        if (file == null) {
            throw new IllegalArgumentException("file can not be null");
        }

        parts = new String[] {file.getAbsolutePath()};
    }

    /**
     * Returns the parts this CommandLineArgument consists of.
     */
    public String[] getParts() {
        return parts;
    }
}
