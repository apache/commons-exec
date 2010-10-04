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
$! Print arguments
$!
$! Crude, but effective. Works even if argument contains un-doubled double-quotes
$ if f$length(P1) .gt. 0 then write sys$output P1
$ if f$length(P2) .gt. 0 then write sys$output P2
$ if f$length(P3) .gt. 0 then write sys$output P3
$ if f$length(P4) .gt. 0 then write sys$output P4
$ if f$length(P5) .gt. 0 then write sys$output P5
$ if f$length(P6) .gt. 0 then write sys$output P6
$ if f$length(P7) .gt. 0 then write sys$output P7
$ if f$length(P8) .gt. 0 then write sys$output P8