$!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
$!
$! Licensed to the Apache Software Foundation (ASF) under one or more
$! contributor license agreements.  See the NOTICE file distributed with
$! this work for additional information regarding copyright ownership.
$! The ASF licenses this file to You under the Apache License, Version 2.0
$! (the "License"); you may not use this file except in compliance with
$! the License.  You may obtain a copy of the License at
$!
$!      http://www.apache.org/licenses/LICENSE-2.0
$!
$! Unless required by applicable law or agreed to in writing, software
$! distributed under the License is distributed on an "AS IS" BASIS,
$! WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
$! See the License for the specific language governing permissions and
$! limitations under the License.
$! 
$!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
$!
$! run an infinite loop so the script will never ever terminate
$!
$! Suppress timeout warning
$ l_msg=f$environment("MESSAGE")
$ SET MESSAGE  /NOFACILITY  /NOIDENTIFICATION       /NOSEVERITY  /NOTEXT
$!
$ SET NOON
$ ON CONTROL_Y THEN GOTO DONE
$ close/nolog OUT
$ open/write OUT [.target]forever.txt ! create the output file
$LOOP:
$   write OUT "."
$   read /prompt="."/time_out=1 sys$command dummy
$ GOTO LOOP
$!
$DONE:
$ close/nolog OUT
$! Restore message settings
$ SET MESSAGE  'l_msg'