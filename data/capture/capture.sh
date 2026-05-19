#!/bin/sh

fn="capture_`date +%s`"

ssh musiker tcpdump -i XHC20 -w $fn.pcap

scp musiker:$fn.pcap .

editcap $fn.pcap $fn.pcapng

rm $fn.pcap

tshark -r $fn.pcapng -x

echo "$fn.pcapng"
