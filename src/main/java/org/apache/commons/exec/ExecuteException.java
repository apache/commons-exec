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

import java.io.IOException;

public class ExecuteException extends IOException {

    /**
     * Comment for <code>serialVersionUID</code>.
     */
    private static final long serialVersionUID = 3256443620654331699L;

    /**
     * Construct a new exception with <code>null</code> as its detail message.
     */
    public ExecuteException() {
        super();
    }

    /**
     * Construct a new exception with the specified detail message.
     * 
     * @param message
     *            The detail message
     */
    public ExecuteException(final String message) {
        super(message);
    }

    /**
     * Construct a new exception with the specified cause and a derived detail
     * message.
     * 
     * @param cause
     *            The underlying cause
     */
    public ExecuteException(final Throwable cause) {
        this((cause == null) ? null : cause.toString(), cause);
    }

    /**
     * Construct a new exception with the specified detail message and cause.
     * 
     * @param message
     *            The detail message
     * @param cause
     *            The underlying cause
     */
    public ExecuteException(final String message, final Throwable cause) {
        super(message + " (Caused by " + cause + ")");
        this.cause = cause; // Two-argument version requires JDK 1.4 or later
    }

    /**
     * The underlying cause of this exception.
     */
    private Throwable cause = null;

    /**
     * Return the underlying cause of this exception (if any).
     */
    public Throwable getCause() {
        return (this.cause);
    }
}
