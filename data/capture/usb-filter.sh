#!/bin/sh

fn="$1"

tshark -r $fn -x -V | awk -f filter.awk
