These integration tests simulate an M-Bus slave. This fake M-Bus slave
is used test an M-Bus master based on jMBus.

In order to run these integration tests the null modem emulator
tty0tty has to be used to create two serial ports that are connected
(one for the server and one for the client).

Steps:

- first change the group ownership of the /dev folder so that you as a
  normal user can create the serial ports. You can execute the
  configure_dev.sh script for that.

- Then run "gradle itest". The integration test will run 
  > jmbus/src/itest/tty0tty/tty0ttyXX /dev/ttyS99 /dev/ttyS100
  /dev/ttyS99 is used by the fake M-Bus slave. /dev/ttyS100 is used by
  the M-Bus master that attempts to read the from the slave.

If you want to run the fake M-Bus slave as a standalone application
you can easily do so from within Eclipse.
