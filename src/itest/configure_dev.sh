#!/bin/sh

# configures the directory /dev 
# in a way that the tool tty0tty 
# can add ttyS* devices without being superuser

sudo chown root:$USER /dev
sudo chmod 775 /dev
