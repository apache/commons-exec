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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class JVMLauncher<R extends Serializable>
{
    private final VmCallable<R> callable;
    private final Collection<URL> userJars;
    private final Consumer<String> consoleHandler;
    private final boolean depThisJvm;
    private final List<String> otherVmOps;

    private Process process;

    JVMLauncher(
            VmCallable<R> callable,
            Consumer<String> consoleHandler,
            Collection<URL> userJars,
            boolean depThisJvm,
            List<String> otherVmOps)
    {
        this.callable = callable;
        this.userJars = userJars;
        this.consoleHandler = consoleHandler;
        this.depThisJvm = depThisJvm;
        this.otherVmOps = otherVmOps;
    }

    public VmFuture<R> startAndGet()
            throws IOException, ClassNotFoundException, JVMException
    {
        return startAndGet(null);
    }

    public VmFuture<R> startAndGet(ClassLoader classLoader)
            throws IOException, ClassNotFoundException, JVMException
    {
        Socket socketClient = null;
        InputStream inputStream = null;
        try {
            socketClient = startAndGetByte();
            inputStream = socketClient.getInputStream();

            VmFuture<R> vmFuture = (VmFuture<R>) Serializables.byteToObject(inputStream);
            if (vmFuture.get() == null) {
                throw new JVMException(vmFuture.getOnFailure());
            }
            return vmFuture;
        }
        finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (socketClient != null) {
                socketClient.close();
            }
        }
    }

    /**
     * send obj callable to execJvm
     */
    private Socket startAndGetByte()
            throws IOException, JVMException
    {
        ServerSocket sock = null;
        try {
            sock = new ServerSocket();
            sock.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
            ProcessBuilder builder = new ProcessBuilder(buildMainArg(sock.getLocalPort(), otherVmOps))
                    .redirectErrorStream(true);

            this.process = builder.start();
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(process.getOutputStream());
                os.write(Serializables.serialize(callable));
            }
            finally {
                if (os != null) {
                    os.close();
                }
            }

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "utf-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    consoleHandler.accept(line);
                }
            }
            finally {
                if (reader != null) {
                    reader.close();
                }
            }
            //---return Socket io Stream
            // If you execute here and jump out of the above where the child process has exited
            // Set a maximum of 3 seconds to wait to prevent the child process from exiting unexpectedly
            // Under normal circumstances, when the child process exits, it has already written back the data. Here, you need to set the abnormal exit time. Maximum waiting time.
            sock.setSoTimeout(3000);
            try {
                return sock.accept();
            }
            catch (SocketTimeoutException e) {
                //todo: if isAlive
//                if (process.isAlive()) {
//                    process.destroy();
//                }
                throw new JVMException("Jvm child process abnormal exit, exit code " + process.exitValue(), e);
            }
        }
        finally {
            if (sock != null) {
                sock.close();
            }
        }
    }

    private String getUserAddClasspath()
    {
        StringBuilder builder = new StringBuilder();
        for (URL url : userJars) {
            builder.append(File.pathSeparator).append(url.getPath());
        }
        return builder.length() == 0 ? "" : builder.substring(File.pathSeparator.length());
    }

    private List<String> buildMainArg(int port, List<String> otherVmOps)
    {
        File java = new File(new File(System.getProperty("java.home"), "bin"), "java");
        List<String> ops = new ArrayList<String>();
        ops.add(java.toString());

        ops.addAll(otherVmOps);

        ops.add("-classpath");

        String userSdkJars = getUserAddClasspath(); //Add additional jar dependencies for users
        if (depThisJvm) {
            ops.add(System.getProperty("java.class.path") + ":" + userSdkJars);
        }
        else {
            ops.add(userSdkJars);
        }

        String javaLibPath = System.getProperty("java.library.path");
        if (javaLibPath != null) {
            ops.add("-Djava.library.path=" + javaLibPath);
        }
        ops.add(JVMLauncher.class.getCanonicalName()); //Child process Main.class
        ops.add(Integer.toString(port));
        return ops;
    }

    public static void main(String[] args)
            throws Exception
    {
        System.out.println("vm start ok ...");
        VmFuture<? extends Serializable> future;

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(System.in);
            System.out.println("vm start init ok ...");
            VmCallable<? extends Serializable> callable = (VmCallable<? extends Serializable>) ois.readObject();
            future = new VmFuture<Serializable>(callable.call());
        }
        catch (Throwable e) {
            future = new VmFuture<Serializable>(getStackTraceAsString(e));
        }
        finally {
            if (ois != null) {
                ois.close();
            }
        }

        OutputStream out = null;
        try {
            out = chooseOutputStream(args);
            out.write(Serializables.serialize(future));
            System.out.println("vm exiting ok ...");
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Read the port passed in args, communicate with the parent process, report the result
     */
    private static OutputStream chooseOutputStream(String[] args)
            throws IOException
    {
        if (args.length > 0) {
            int port = Integer.parseInt(args[0]);
            Socket sock = new Socket();
            sock.connect(new InetSocketAddress(InetAddress.getLocalHost(), port));
            return sock.getOutputStream();
        }
        else {
            return System.out;
        }
    }

    /**
     * Returns a string containing the result of {@link Throwable#toString() toString()}
     */
    private static String getStackTraceAsString(Throwable throwable)
    {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
