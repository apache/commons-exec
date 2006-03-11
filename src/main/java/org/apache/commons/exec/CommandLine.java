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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * CommandLine objects help handling command lines specifying processes to
 * execute. The class can be used to define a command line as nested elements or
 * as a helper to define a command line by an application.
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
     * @param toProcess
     *            the line: the first element becomes the executable, the rest
     *            the arguments
     */
    public CommandLine(final String toProcess) {
        super();
        String[] tmp = translateCommandline(toProcess);
        if (tmp != null && tmp.length > 0) {
            setExecutable(tmp[0]);
            for (int i = 1; i < tmp.length; i++) {
                createArgument(tmp[i]);
            }
        }
    }

    /**
     * Create an empty command line.
     */
    public CommandLine() {
        super();
    }

    /**
     * Creates an argument object.
     * <p>
     * Each commandline object has at most one instance of the argument class.
     * This method calls <code>this.createArgument(false)</code>.
     * </p>
     * 
     * @return the argument object.
     */
    private CommandLineArgument createArgument(final String value) {
        CommandLineArgument argument = new CommandLineArgument(value);
        arguments.addElement(argument);
        return argument;
    }

    public void setExecutable(final String executable) {
        if (executable == null || executable.length() == 0) {
            return;
        }
        this.executable = executable.replace('/', File.separatorChar).replace(
                '\\', File.separatorChar);
    }

    public String getExecutable() {
        return executable;
    }

    public void addArguments(final String[] line) {
        for (int i = 0; i < line.length; i++) {
            createArgument(line[i]);
        }
    }

    public void addArgument(final String arg) {
        createArgument(arg);
    }

    public String[] getCommandline() {
        List commands = new LinkedList();
        ListIterator list = commands.listIterator();
        addCommandToList(list);
        final String[] result = new String[commands.size()];
        return (String[]) commands.toArray(result);
    }

    /**
     * Add the entire command, including (optional) executable to a list.
     * 
     * @param list
     */
    private void addCommandToList(final ListIterator list) {
        if (executable != null) {
            list.add(executable);
        }
        addArgumentsToList(list);
    }

    public String[] getArguments() {
        List result = new ArrayList(arguments.size() * 2);
        addArgumentsToList(result.listIterator());
        String[] res = new String[result.size()];
        return (String[]) result.toArray(res);
    }

    /**
     * append all the arguments to the tail of a supplied list
     * 
     * @param list
     */
    private void addArgumentsToList(final ListIterator list) {
        for (int i = 0; i < arguments.size(); i++) {
            CommandLineArgument arg = (CommandLineArgument) arguments
                    .elementAt(i);
            String[] s = arg.getParts();
            if (s != null) {
                for (int j = 0; j < s.length; j++) {
                    list.add(s[j]);
                }
            }
        }
    }

    /**
     * Stringify operator returns the command line as a string.
     * 
     * @return the command line
     */
    public String toString() {
        return toString(getCommandline());
    }

    /**
     * Put quotes around the given String if necessary.
     * <p>
     * If the argument doesn't include spaces or quotes, return it as is. If it
     * contains double quotes, use single quotes - else surround the argument by
     * double quotes.
     * </p>
     * 
     */
    public static String quoteArgument(final String argument) {
        final StringBuffer buf = new StringBuffer();
        if (argument.indexOf(DOUBLE_QUOTE) > -1) {
            if (argument.indexOf(SINGLE_QUOTE) > -1) {
                throw new IllegalArgumentException(
                        "Can\'t handle single and double quotes in same argument");
            } else {
                return buf.append(SINGLE_QUOTE).append(argument).append(SINGLE_QUOTE).toString();
            }
        } else if (argument.indexOf(SINGLE_QUOTE) > -1 || argument.indexOf(" ") > -1) {
            return buf.append(DOUBLE_QUOTE).append(argument).append(DOUBLE_QUOTE).toString();
        } else {
            return argument;
        }
    }

    /**
     * Quotes the parts of the given array in way that makes them usable as
     * command line arguments.
     * 
     * @return empty string for null or no command, else every argument split by
     *         spaces and quoted by quoting rules
     */
    public static String toString(final String[] line) {
        // empty path return empty string
        if (line == null || line.length == 0) {
            return "";
        }

        // path containing one or more elements
        final StringBuffer result = new StringBuffer();
        for (int i = 0; i < line.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(quoteArgument(line[i]));
        }
        return result.toString();
    }

    /**
     * Crack a command line.
     * 
     * @param toProcess
     *            the command line to process
     * @return the command line broken into strings. An empty or null toProcess
     *         parameter results in a zero sized array
     */
    public static String[] translateCommandline(final String toProcess) {
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
            throw new IllegalArgumentException(
                    "Unbalanced quotes in " + toProcess);
        }

        String[] args = new String[v.size()];
        v.copyInto(args);
        return args;
    }

    /**
     * size operator. This actually creates the command line, so it is not a
     * zero cost operation.
     * 
     * @return number of elements in the command, including the executable
     */
    public int size() {
        return getCommandline().length;
    }

    /**
     * Generate a deep clone of the contained object.
     * 
     * @return a clone of the contained object
     * @throws CloneNotSupportedException 
     */
    public Object clone() throws CloneNotSupportedException {
        CommandLine c = (CommandLine) super.clone();
        c.arguments = (Vector) arguments.clone();
        return c;
    }

    public void clear() {
        executable = null;
        arguments.removeAllElements();
    }

    public void clearArgs() {
        arguments.removeAllElements();
    }
}
