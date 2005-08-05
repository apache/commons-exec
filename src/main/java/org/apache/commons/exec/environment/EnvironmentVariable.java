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

package org.apache.commons.exec.environment;

import java.io.File;


/**
 * representation of a single env value
 */
public final class EnvironmentVariable {

    public static EnvironmentVariable createEnvironmentVariable(
            final String keyAndValue) {
        return new EnvironmentVariable(keyAndValue);
    }

    public static EnvironmentVariable createEnvironmentVariable(
            final String key, final String value) {
        return new EnvironmentVariable(key, value);
    }

    /**
     * env key and value pair; everything gets expanded to a string during
     * assignment
     */
    private String value;

    private String key;

    /**
     * Constructor for variable
     */
    private EnvironmentVariable(final String key, final String value) {
        if (key == null) {
            throw new NullPointerException("key can not be null");
        }
        if (value == null) {
            throw new NullPointerException("value can not be null");
        }
        this.key = key;
        this.value = value;
    }

    private EnvironmentVariable(final String keyAndValue) {
        int index = keyAndValue.indexOf('=');
        if (index == -1) {
            throw new IllegalArgumentException(
                    "Environment variable for this platform "
                            + "must contain an equals sign ('=')");
        }

        this.key = keyAndValue.substring(0, index);
        this.value = keyAndValue.substring(index + 1);
    }

    /**
     * set the key
     * 
     * @param key
     *            string
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * set the value
     * 
     * @param value
     *            string value
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * key accessor
     * 
     * @return key
     */
    public String getKey() {
        return this.key;
    }

    /**
     * value accessor
     * 
     * @return value
     */
    public String getValue() {
        return this.value;
    }

    /**
     * stringify path and assign to the value. The value will contain all path
     * elements separated by the appropriate separator
     * 
     * @param path
     *            path
     */
    /*
     * public void setPath(Path path) { this.value = path.toString(); }
     */

    /**
     * get the absolute path of a file and assign it to the value
     * 
     * @param file
     *            file to use as the value
     */
    public void setFile(final File file) {
        this.value = file.getAbsolutePath();
    }

    /**
     * get the assigment string This is not ready for insertion into a property
     * file without following the escaping rules of the properties class.
     * 
     * @return a string of the form key=value.
     *             if key or value are unassigned
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(key.trim());
        sb.append("=").append(value.trim());
        return sb.toString();
    }

    public boolean equals(final Object o) {
        if (!(o instanceof EnvironmentVariable)) {
            return false;
        }

        EnvironmentVariable envVar = (EnvironmentVariable) o;

        return key.equals(envVar.key) && value.equals(envVar.value);
    }
}