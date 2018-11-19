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
package org.apache.commons.exec.jvm;

import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * JVMLauncher builder
 */
public class JVMLaunchers
{
    private JVMLaunchers() {}

    public static class VmBuilder<T extends Serializable>
    {
        private VmCallable<T> callable;
        private boolean depThisJvm = true;
        private Consumer<String> consoleHandler;
        private final List<URL> tmpJars = new ArrayList<URL>();
        private final List<String> otherVmOps = new ArrayList<String>();

        public VmBuilder<T> setCallable(VmCallable<T> callable)
        {
            this.callable = requireNonNull(callable, "callable is null");
            return this;
        }

        public VmBuilder<T> setConsole(Consumer<String> consoleHandler)
        {
            this.consoleHandler = requireNonNull(consoleHandler, "consoleHandler is null");
            return this;
        }

        /**
         * Do not rely on the dependency of the current classloader of any sub-process.
         * Please use this option carefully. {@link java.lang.ClassNotFoundException} may occur.
         */
        public VmBuilder<T> notDepThisJvmClassPath()
        {
            depThisJvm = false;
            return this;
        }

        public VmBuilder<T> addUserURLClassLoader(URLClassLoader vmClassLoader)
        {
            ClassLoader classLoader = vmClassLoader;
            while (classLoader instanceof URLClassLoader) {
                Collections.addAll(tmpJars, ((URLClassLoader) classLoader).getURLs());
                classLoader = classLoader.getParent();
            }
            return this;
        }

        public VmBuilder<T> addUserjars(Collection<URL> jars)
        {
            tmpJars.addAll(jars);
            return this;
        }

        public VmBuilder<T> setXms(String xms)
        {
            otherVmOps.add("-Xms" + xms);
            return this;
        }

        public VmBuilder<T> setXmx(String xmx)
        {
            otherVmOps.add("-Xmx" + xmx);
            return this;
        }

        public JVMLauncher<T> build()
        {
            requireNonNull(consoleHandler, "setConsole(Consumer<String> consoleHandler) not setting");
            requireNonNull(callable, "callable not setting");
            return new JVMLauncher<T>(callable, consoleHandler, tmpJars, depThisJvm, otherVmOps);
        }
    }

    public static <T extends Serializable> VmBuilder<T> newJvm()
    {
        return new VmBuilder<T>();
    }

    private static <V> V requireNonNull(V obj, String message)
    {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }
}
