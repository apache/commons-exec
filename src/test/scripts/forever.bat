@ECHO OFF

REM Little batch file to run nearly foerver
REM see https://malektips.com/dos0017.html
REM
REM Licensed to the Apache Software Foundation (ASF) under one or more
REM contributor license agreements.  See the NOTICE file distributed with
REM this work for additional information regarding copyright ownership.
REM The ASF licenses this file to You under the Apache License, Version 2.0
REM (the "License"); you may not use this file except in compliance with
REM the License.  You may obtain a copy of the License at
REM
REM      https://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.

REM run an infinite loop so the script will never ever terminate on its behalf
REM and append a '.' after each second

:LOOP
  ECHO . >> .\target\forever.txt
  @ping 127.0.0.1 -n 2 -w 1000 > nul
GOTO LOOP