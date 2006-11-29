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

package org.apache.commons.exec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * CommandLine objects help handling command lines specifying processes to
 * execute. The class can be used to a command line by an application.
 */
public class CommandLine {

    private static final String SINGLE_QUOTE = "\'";

    private static final String DOUBLE_QUOTE = "\"";

    /**
     * The arguments of the command.
     */
    private Vector arguments = new Vector();

    /**
     * The program to execute.
     */
    private String executable = null;

    /**
     * Create a command line from a string.
     * 
     * @param line
     *            the line: the first element becomes the executable, the rest
     *            the arguments
     * @throws IllegalArgumentException If line is null or all whitespace
     */
    public static CommandLine parse(final String line) {
        if (line == null) {
            throw new IllegalArgumentException("Command line can not be null");
        } else if (line.trim().length() == 0) {
            throw new IllegalArgumentException("Command line can not be empty");
        } else {
            String[] tmp = translateCommandline(line);

            CommandLine cl = new CommandLine(tmp[0]);
            for (int i = 1; i < tmp.length; i++) {
                cl.addArgument(tmp[i]);
            }

            return cl;
        }
    }

    /**
     * Create a command line without any arguments.
     */
    public CommandLine(String executable) {
        setExecutable(executable);
    }

    /**
     * Create a command line without any arguments.
     */
    public CommandLine(File executable) {
        setExecutable(executable.getAbsolutePath());
    }

    private void setExecutable(final String executable) {
        if (executable == null) {
            throw new IllegalArgumentException("Executable can not be null");
        } else if(executable.trim().length() == 0) {
            throw new IllegalArgumentException("Executable can not be empty");
        } else {
             this.executable = executable.replace('/', File.separatorChar).replace(
                '\\', File.separatorChar);
        }
    }

    /**
     * Returns the executable
     * 
     * @return The executable
     */
    public String getExecutable() {
        return executable;
    }

    /**
     * Add multiple arguments
     * 
     * @param arguments An array of arguments
     * @return The command line itself
     */
    public CommandLine addArguments(final String[] arguments) {
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                addArgument(arguments[i]);
            }
        }
        
        return this;
    }

    /**
     * Add multiple arguments. Handles parsing of quotes and whitespace.
     * 
     * @param arguments An string containing multiple arguments. 
     * @return The command line itself
     */
    public CommandLine addArguments(final String arguments) {
        if (arguments != null) {
            String[] argmentsArray = translateCommandline(arguments);
    
            addArguments(argmentsArray);
        }
        
        return this;
    }

    /**
     * Add a single argument. Handles quoting.
     * @param argument The argument to add
     * @throws IllegalArgumentException If argument contains both single and double quotes
     */
    public void addArgument(final String argument) {
        if (argument == null)
            return;

        arguments.add(quoteArgument(argument));
    }

    /**
     * Returns the quoted arguments 
     * @return The quoted arguments
     */
    public String[] getArguments() {
        String[] res = new String[arguments.size()];
        return (String[]) arguments.toArray(res);
    }

    /**
     * Put quotes around the given String if necessary.
     * <p>
     * If the argument doesn't include spaces or quotes, return it as is. If it
     * contains double quotes, use single quotes - else surround the argument by
     * double quotes.
     * </p>
     * 
     * @throws IllegalArgumentException If argument contains both types of quotes
     */
    private static String quoteArgument(final String argument) {
        String cleanedArgument = argument.trim();
        
        while(cleanedArgument.startsWith(SINGLE_QUOTE) || cleanedArgument.startsWith(DOUBLE_QUOTE)) {
            cleanedArgument = cleanedArgument.substring(1);
        }
        while(cleanedArgument.endsWith(SINGLE_QUOTE) || cleanedArgument.endsWith(DOUBLE_QUOTE)) {
            cleanedArgument = cleanedArgument.substring(0, cleanedArgument.length() - 1);
        }

        
        final StringBuffer buf = new StringBuffer();
        if (cleanedArgument.indexOf(DOUBLE_QUOTE) > -1) {
            if (cleanedArgument.indexOf(SINGLE_QUOTE) > -1) {
                throw new IllegalArgumentException(
                        "Can't handle single and double quotes in same argument");
            } else {
                return buf.append(SINGLE_QUOTE).append(cleanedArgument).append(
                        SINGLE_QUOTE).toString();
            }
        } else if (cleanedArgument.indexOf(SINGLE_QUOTE) > -1
                || cleanedArgument.indexOf(" ") > -1) {
            return buf.append(DOUBLE_QUOTE).append(cleanedArgument).append(
                    DOUBLE_QUOTE).toString();
        } else {
            return cleanedArgument;
        }
    }

    /**
     * Crack a command line.
     * 
     * @param toProcess
     *            the command line to process
     * @return the command line broken into strings. An empty or null toProcess
     *         parameter results in a zero sized array
     */
    private static String[] translateCommandline(final String toProcess) {
        if (toProcess == null || toProcess.length() == 0) {
            // no command? no string
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        Vector v = new Vector();
        StringBuffer current = new StringBuffer();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
            case inQuote:
                if ("\'".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            case inDoubleQuote:
                if ("\"".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            default:
                if ("\'".equals(nextTok)) {
                    state = inQuote;
                } else if ("\"".equals(nextTok)) {
                    state = inDoubleQuote;
                } else if (" ".equals(nextTok)) {
                    if (lastTokenHasBeenQuoted || current.length() != 0) {
                        v.addElement(current.toString());
                        current = new StringBuffer();
                    }
                } else {
                    current.append(nextTok);
                }
                lastTokenHasBeenQuoted = false;
                break;
            }
        }

        if (lastTokenHasBeenQuoted || current.length() != 0) {
            v.addElement(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new IllegalArgumentException("Unbalanced quotes in "
                    + toProcess);
        }

        String[] args = new String[v.size()];
        v.copyInto(args);
        return args;
    }

    /**
     * Returns the command line as an array of strings, correctly quoted
     * for use in executing the command.
     * @return The command line as an string array
     */
    public String[] toStrings() {
        final String[] result = new String[arguments.size() + 1];
        result[0] = executable;

        int index = 1;
        for (Iterator iter = arguments.iterator(); iter.hasNext();) {
            result[index] = (String) iter.next();

            index++;
        }

        return result;
    }

    /**
     * Stringify operator returns the command line as a string.
     * 
     * @return the command line
     */
    public String toString() {
        String[] strings = toStrings();

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < strings.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(strings[i]);
        }

        return sb.toString();
    }
}
