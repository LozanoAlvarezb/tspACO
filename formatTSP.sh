#! /bin/bash

FILE=$1
NEWFILE=$(echo "$FILE" | cut -f 1 -d '.')

tail -n +9 "$FILE" | tail -r | tail -n +2 | tail -r | cut -d' ' -f2- > $NEWFILE

cat $NEWFILE

