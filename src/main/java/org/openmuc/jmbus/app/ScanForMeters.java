/*
 * Copyright 2010-16 Fraunhofer ISE
 *
 * This file is part of jMBus.
 * For more information visit http://www.openmuc.org
 *
 * jMBus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jMBus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jMBus.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.jmbus.app;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.openmuc.jmbus.MBusSap;
import org.openmuc.jmbus.ScanSecondaryAddress;

public class ScanForMeters {

    private final static int MIN_ARGS_LENGTH = 1;
    private final static int MAX_ARGS_LENGTH = 5;
    private final static int WILDCARD_MASK_LENGTH = 8;

    private static void printUsage() {
        System.out.println(
                "SYNOPSIS\n\torg.openmuc.jmbus.app.ScanForMeters <serial_port> [-b <baud_rate>] [-s [<wildcard_mask>]]");
        System.out.println(
                "DESCRIPTION\n\tScans the primary addresses 0 to 250 for connected meters by sending REQ_UD2 packets and waiting for a response.");
        System.out.println("OPTIONS");
        System.out.println(
                "\t<serial_port>\n\t    The serial port used for communication. Examples are /dev/ttyS0 (Linux) or COM1 (Windows)\n");
        System.out.println("\t-b <baud_rate>\n\t    The baud rate used to connect to the meter. Default is 2400.\n");
        System.out.println("\t-t <timeout>\n\t    The scan timeout in milli seconds. Default is 1000 ms.\n");
        System.out.println("\t-s\n\t Scan for secondary addresses. Examples are -s or -s 15ffffff\n");
    }

    public static void main(String[] args) {

        int argsLength = args.length;
        int baudRate = 2400;
        int timeout = 1000;
        String wildcardMask = "ffffffff";

        boolean scanSecondaryAddress = false;

        if (argsLength < MIN_ARGS_LENGTH || argsLength > MAX_ARGS_LENGTH) {
            error("Error: too few arguments.", true);
        }

        String serialPortName = args[0];

        for (int i = 1; i < args.length; ++i) {

            if (args[i].equals("-b")) {
                try {
                    baudRate = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    error("Error, the <baud_rate> parameter is not an integer value.", false);
                } catch (NullPointerException e) {
                    error("Error, no baudrate behind -b.", false);
                }
            }

            if (args[i].equals("-t")) {
                try {
                    timeout = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    error("Error, the <timeout> parameter is not an integer value.", false);
                } catch (NullPointerException e) {
                    error("Error, no timeout behind -t.", false);
                }
            }

            if (args[i].equals("-s")) {
                scanSecondaryAddress = true;
                if (i < argsLength - 1) {
                    if (args[++i].startsWith("-")) {
                        --i;
                    }
                    else {
                        wildcardMask = args[i];
                        if (wildcardMask.length() != WILDCARD_MASK_LENGTH) {
                            error("Error, allowed wilcard mask length is " + WILDCARD_MASK_LENGTH + " charactors.",
                                    false);
                        }
                    }
                }
            }
        }

        MBusSap mBusSap = new MBusSap(serialPortName, baudRate);
        try {
            mBusSap.open();
            mBusSap.setTimeout(timeout);

            System.out.println("Scanning address: ");

            if (scanSecondaryAddress) {
                ScanSecondaryAddress.scan(mBusSap, wildcardMask);
            }
            else {
                scanPrimaryAddresses(mBusSap);
            }

        } catch (IOException e2) {
            System.out.println("Failed to open serial port: " + e2.getMessage());
            return;
        } finally {
            mBusSap.close();
        }
        System.out.println();
        System.out.println("Scan finished.");

    }

    static void scanPrimaryAddresses(MBusSap mBusSap) {

        for (int i = 0; i <= 250; i++) {

            System.out.print(i + ",");
            try {
                mBusSap.linkReset(i);
                try {
                    Thread.sleep(50); // for slow slaves
                } catch (InterruptedException e) {
                    error("Thread sleep fails.\n" + e.getMessage(), false);
                }
                mBusSap.read(i);
            } catch (TimeoutException e) {
                continue;
            } catch (IOException e) {
                System.out.println();
                System.out.println("Error reading meter at primary address " + i + ": " + e.getMessage());
                System.out.print("Scanning address: ");
                continue;
            }
            System.out.println();
            System.out.println("Found device at primary address " + i + ".");
            System.out.print("Scanning address: ");
        }
    }

    private static void error(String errMsg, boolean printUsage) {
        System.err.println(errMsg + "\n");
        if (printUsage) {
            printUsage();
        }
        System.exit(1);
    }

}
