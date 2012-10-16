#!/bin/zsh

setopt extended_glob
cloc app/**/*coffee app/**/*.scala app/**/*.scala.html \
app/**/*.less~app/**/bootstrap*.less~app/**/bootstrap/_*.less
