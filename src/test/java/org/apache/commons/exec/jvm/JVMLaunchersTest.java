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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class JVMLaunchersTest
{

    @Test
    public void newJvm()
            throws Exception
    {
        int exitCode = testNewJvm();
        Assert.assertEquals(exitCode, 1);
    }

    private static int testNewJvm()
            throws JVMException, IOException, ClassNotFoundException
    {
        JVMLauncher<Integer> launcher = JVMLaunchers.<Integer>newJvm()
                .setCallable(new VmCallable<Integer>()
                {
                    @Override
                    public Integer call()
                            throws Exception
                    {
                        System.out.println("************ Compile start ***************");
                        TimeUnit.SECONDS.sleep(1);
                        System.out.println("************ Compile stop ***************");
                        return 1;
                    }
                })
                .addUserjars(Collections.<URL>emptyList())
                .setConsole(new Consumer<String>()
                {
                    @Override
                    public void accept(String msg)
                    {
                        System.out.println(msg);
                    }
                })
                .setXms("16m")
                .setXmx("16m")
                .build();
        VmFuture<Integer> out = launcher.startAndGet();
        return out.get();
    }
}