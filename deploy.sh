#!/bin/bash

scp ./target/wombats.jar $1:~
ssh $1 'sudo /etc/init.d/wombats restart'
