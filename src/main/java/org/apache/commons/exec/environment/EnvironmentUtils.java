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
 *
 */

package org.apache.commons.exec.environment;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.exec.OS;

/**
 * Wrapper for environment variables.
 */
public class EnvironmentUtils
{

	private static final DefaultProcessingEnvironment PROCESSING_ENVIRONMENT_IMPLEMENTATION;
	
	static {
        if (OS.isFamilyOpenVms()) {
        	PROCESSING_ENVIRONMENT_IMPLEMENTATION = new OpenVmsProcessingEnvironment();
        } else {
        	PROCESSING_ENVIRONMENT_IMPLEMENTATION = new DefaultProcessingEnvironment();
        }
	}
	
    /**
     * Disable constructor.
     */
    private EnvironmentUtils() {

    }

    /**
     * Get the variable list as an array.
     *
     * @param environment the environment to use, may be <code>null</code>
     * @return array of key=value assignment strings or <code>null</code> if and only if
     *     the input map was <code>null</code>
     */
    public static String[] toStrings(Map environment) {
        if (environment == null) {
            return null;
        }
        String[] result = new String[environment.size()];
        int i = 0;
        for (Iterator iter = environment.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();

            result[i] = entry.getKey().toString() + "=" + entry.getValue().toString();
            i++;
        }
        return result;
    }

    /**
     * Find the list of environment variables for this process. The returned map preserves
     * the casing of a variable's name on all platforms but obeys the casing rules of the
     * current platform during lookup, e.g. key names will be case-insensitive on Windows
     * platforms.
     *
     * @return a map containing the environment variables, may be empty but never <code>null</code>
     * @throws IOException the operation failed
     */
    public static Map getProcEnvironment() throws IOException {
    	return PROCESSING_ENVIRONMENT_IMPLEMENTATION.getProcEnvironment();
    }

    /**
     * Add a key/value pair to the given environment.
     * If the key matches an existing key, the previous setting is replaced.
     *
     * @param environment the current environment
     * @param keyAndValue the key/value pair 
     */
    public static void addVariableToEnvironment(Map environment, String keyAndValue) {
		String[] parsedVariable = parseEnvironmentVariable(keyAndValue);		
		environment.put(parsedVariable[0], parsedVariable[1]);
	}
    
    /**
     * Split a key/value pair into a String[]. It is assumed
     * that the ky/value pair contains a '=' character.
     *
     * @param keyAndValue the key/value pair
     * @return a String[] containing the key and value
     */
    private static String[] parseEnvironmentVariable(final String keyAndValue) {
        int index = keyAndValue.indexOf('=');
        if (index == -1) {
            throw new IllegalArgumentException(
                    "Environment variable for this platform "
                            + "must contain an equals sign ('=')");
        }

        String[] result = new String[2];
        result[0] = keyAndValue.substring(0, index);
        result[1] = keyAndValue.substring(index + 1);

        return result;
    }
    
}
