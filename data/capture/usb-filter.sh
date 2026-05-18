#!/bin/sh

fn="$1"

tshark -r $fn -x | awk -f filter.awk
