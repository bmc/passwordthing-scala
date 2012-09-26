#!/bin/zsh

setopt extended_glob
wc -l app/**/*coffee app/**/*.scala app/**/*.scala.html app/**/*.less~app/**/bootstrap*
