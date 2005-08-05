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

/**
 * CommandLine objects help handling command lines specifying processes to
 * execute. The class can be used to define a command line as nested elements or
 * as a helper to define a command line by an application.
 */
public interface CommandLine {

    /**
     * Sets the executable to run. All file separators in the string are
     * converted to the platform specific value
     */
    void setExecutable(final String executable);

    /**
     * Get the executable.
     * 
     * @return the program to run -null if not yet set
     */
    String getExecutable();

    /**
     * Append the arguments to the existing command.
     * 
     * @param line
     *            an array of arguments to append
     */
    void addArguments(final String[] line);

    void addArgument(final String arg);

    /**
     * Returns the executable and all defined arguments.
     */
    String[] getCommandline();

    /**
     * Returns all arguments defined by <code>addLine</code>,
     * <code>addValue</code> or the argument object.
     */
    String[] getArguments();

    /**
     * Clear out the whole command line.
     */
    void clear();

    /**
     * Clear out the arguments but leave the executable in place for another
     * operation.
     */
    void clearArgs();

}