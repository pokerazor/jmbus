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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.HexConverter;
import org.openmuc.jmbus.MBusSap;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;

public class ReadMeter {

    private static void printUsage() {
        System.out.println(
                "SYNOPSIS\n\torg.openmuc.jmbus.app.ReadMeter <serial_port> (<primary_address> | <secondary_address>) [-b <baud_rate>] [-t <timeout>]");
        // [<data_record_to_read> ...]");
        System.out.println(
                "DESCRIPTION\n\tReads a meter connected to the given serial port and prints the received data to stdout. Errors are printed to stderr. The default behaviour is to send a REQ_UD to the meter and print all the data records from the RSP_UD frame received. If a list of DIB/VIBs is specified this application will first send a SND_UD frame with CI-Field 51h to select the given data records for read out.");
        System.out.println("OPTIONS");
        System.out.println(
                "\t<serial_port>\n\t    The serial port used for communication. Examples are /dev/ttyS0 (Linux) or COM1 (Windows)\n");
        System.out.println(
                "\t<primary_address>\n\t    The primary address of the meter. Primary addresses range from 0 to 255. Regular primary address range from 1 to 250.\n");
        System.out.println(
                "\t<secondary_address>\n\t    The secondary address of the meter. Secondary addresses are 8 bytes long and shall be entered in hexadecimal form (e.g. 3a453b4f4f343423)\n");
        System.out.println("\t-b <baud_rate>\n\t    The baud rate used to connect to the meter. Default is 2400.\n");
        System.out.println(
                "\t-t <timeout>\n\t    The timeout, in milli seconds, between send and receive. Default is 500 ms\n");
        // System.out.println(
        // "\t-d <data_record_to_read>\n\t Data record to select for read out. Format is <dib>:<vib> e.g. -d
        // 8801:28\n");
    }

    public static void main(String[] args) {

        int argsLength = args.length;
        if (argsLength < 2) {
            error("Error: too few arguments.", true);
            printUsage();
            System.exit(1);
        }

        String serialPortName = args[0];
        String address = args[1];
        int primaryAddress = 0;
        SecondaryAddress secondaryAddress = null;
        int addrLength = address.length();

        if (addrLength > 3) {
            if (addrLength != 16) {
                error("Error: the <secondary_address> has the wrong length. Should be 16 but is " + addrLength, true);
            }
            try {
                secondaryAddress = SecondaryAddress.getFromLongHeader(HexConverter.fromShortHexString(address), 0);
            } catch (NumberFormatException e) {
                error("Error: the <secondary_address> parameter contains non hexadecimal character.", true);
            }
        }
        else {
            try {
                primaryAddress = Integer.parseInt(address);
            } catch (NumberFormatException e) {
                error("Error: the <primary_address> parameter is not an integer value.", true);
            }
        }

        List<DataRecord> dataRecordsToSelectForReadout = new ArrayList<DataRecord>();

        int baudRate = 2400;
        int timeout = 500;
        if (argsLength > 2) {
            int argsIndex = 2;
            while (argsIndex < argsLength) {

                if (args[argsIndex].equals("-b")) {
                    try {
                        baudRate = parseIntegerArg(args, argsLength, argsIndex);
                    } catch (NumberFormatException e) {
                        error("Error: the <baud_rate> parameter is not an integer value.", true);
                    }
                    argsIndex += 2;
                }
                else if (args[argsIndex].equals("-t")) {
                    try {
                        timeout = parseIntegerArg(args, argsLength, argsIndex);
                    } catch (NumberFormatException e) {
                        error("Error: the <timeout> parameter is not an integer value.", true);
                    }
                    argsIndex += 2;
                }
                // else if (args[argsIndex].equals("-d")) {
                //
                // if (argsLength < argsIndex + 2) {
                // System.out.println("1");
                // printUsage();
                // System.exit(1);
                // }
                // String[] dibAndVib = args[argsIndex + 1].split(":");
                // if (dibAndVib.length != 2) {
                // error("Error, the <data_record_to_read> has the wrong format. Correct format is -d <dib>:<vib> e.g.
                // -d 8801:28",
                // true);
                // }
                // argsIndex += 2;
                // }
                else {
                    error("Error, unknown argument.", true);
                }

                // TODO
                // dataRecordsToSelectForReadout.add(new DataRecord(HexConverter.fromShortHexString(dibAndVib[0]),
                // HexConverter.fromShortHexString(dibAndVib[1]), new byte[] {}, 0));
            }
        }
        MBusSap mBusSap = new MBusSap(serialPortName, baudRate);
        mBusSap.setTimeout(timeout);

        try {
            mBusSap.open();
        } catch (IOException e2) {
            error("Failed to open serial port: " + e2.getMessage(), false);
        }

        VariableDataStructure variableDataStructure = null;
        if (secondaryAddress != null) {
            try {
                mBusSap.selectComponent(secondaryAddress);
            } catch (IOException e) {
                mBusSap.close();
                error("Error selecting secondary address: " + e.getMessage(), false);
            } catch (TimeoutException e) {
                mBusSap.close();
                error("Selecting secondary address attempt timed out.", false);
            }
            primaryAddress = 0xfd;
        }
        else {
            try {
                mBusSap.linkReset(primaryAddress);
            } catch (IOException e) {
                mBusSap.close();
                error("Error resetting link (SND_NKE): " + e.getMessage(), false);
            } catch (TimeoutException e) {
                mBusSap.close();
                error("Resetting link (SND_NKE) attempt timed out.", false);
            }
            try {
                Thread.sleep(100); // for slow slaves
            } catch (InterruptedException e) {
                error("Thread sleep fails.\n" + e.getMessage(), false);
            }
        }
        if (dataRecordsToSelectForReadout.size() > 0) {
            try {
                mBusSap.selectForReadout(primaryAddress, dataRecordsToSelectForReadout);
            } catch (IOException e) {
                mBusSap.close();
                error("Error selecting data record for readout: " + e.getMessage(), false);
            } catch (TimeoutException e) {
                mBusSap.close();
                error("Selecting data record for readout timed out.", false);
            }
        }

        do {
            try {
                variableDataStructure = mBusSap.read(primaryAddress);
            } catch (IOException e) {
                mBusSap.close();
                error("Error reading meter: " + e.getMessage(), false);
            } catch (TimeoutException e) {
                mBusSap.close();
                error("Read attempt timed out.", false);
            }

            if (dataRecordsToSelectForReadout.size() > 0) {
                try {
                    mBusSap.resetReadout(primaryAddress);
                } catch (IOException e) {
                    System.err.println("Error resetting meter for standard readout: " + e.getMessage());
                } catch (TimeoutException e) {
                    System.err.println("Resetting meter for standard readout timed out.");
                }
            }

            System.out.println(variableDataStructure.toString());
            System.out.println();

        } while (variableDataStructure.moreRecordsFollow());

        mBusSap.close();

    }

    private static int parseIntegerArg(String[] args, int argsLength, int argsIndex) throws NumberFormatException {
        int ret = 0;
        if (argsLength < argsIndex + 2) {
            error("Error: missing expected parameter value", false);
        }
        try {
            ret = Integer.parseInt(args[++argsIndex]);
        } catch (NumberFormatException e) {
            error("Error: the parameter value is not an integer value.", false);
        }
        return ret;
    }

    private static void error(String errMsg, boolean printUsage) {
        System.err.println(errMsg + "\n");
        if (printUsage) {
            printUsage();
        }
        System.exit(1);
    }

}
