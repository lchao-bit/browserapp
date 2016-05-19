#!/bin/sh
#sleep 30
rfkill unblock bluetooth
sleep 1
systemctl start bluetooth
sleep 1
/home/bluez-5.18/test/test-nap br0 &
sleep 1
brctl addbr br0
sleep 2
ifconfig br0 192.168.13.1
