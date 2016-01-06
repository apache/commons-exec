#!/bin/sh

Echo simulate timeout
cat /dev/urandom &
cat /dev/urandom >&2 &
sleep 15