/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.app;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.SecondaryAddressListener;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jrxtx.SerialPortTimeoutException;

class CliConnection {

    private static void printWriteExample() {
        System.out.println("Example for writing to a meter:");
        System.out.println(
                "\tChange primary address: org.openmuc.jmbus.app.JmbusApp -w -sp /dev/ttyUSB0 -a <old_primary_address> -dif 01 -vif 7a -data <data_new_primary_address>");
        System.out.println(
                "\tChange primary address from 20 to 26: org.openmuc.jmbus.app.JmbusApp -w  -sb /dev/ttyUSB0 -a 20 -dif 01 -vif 7a -data 1a");
        System.out.println(
                "\tSet primary address with secondary address: org.openmuc.jmbus.app.JmbusApp /dev/ttyUSB0 -a <secondary_address> -dif 01  -vif 7a -data <data_primary_address>");
        System.out.println(
                "\tSet primary address to 47 (0x2f) with secondary address 3a453b4f4f343423: org.openmuc.jmbus.app.JmbusApp -sb /dev/ttyUSB0 -a 3a453b4f4f343423 -dif 01 -vif 7a -data 2f");
        System.out.println("\n\n");
    }

    public static void write(ConsoleLineParser cliParser, MBusConnection mBusConnection, CliPrinter cliPrinter) {
        int primaryAddress = cliParser.getPrimaryAddress();
        SecondaryAddress secondaryAddress = cliParser.getSecondaryAddress();

        byte[] data = cliParser.getData();
        byte[] dif = cliParser.getDif();
        byte[] vif = cliParser.getVif();

        if (dif.length == 0 || vif.length == 0) {
            printWriteExample();
            cliPrinter.printError("No dif or vif setted.", true);
        }

        VerboseMessageListenerImpl messageListener = new VerboseMessageListenerImpl(cliPrinter);
        mBusConnection.setVerboseMessageListener(messageListener);

        if (secondaryAddress != null) {
            try {
                mBusConnection.selectComponent(secondaryAddress);
            } catch (SerialPortTimeoutException e) {
                mBusConnection.close();
                cliPrinter.printError("Selecting secondary address attempt timed out.", false);
            } catch (IOException e) {
                mBusConnection.close();
                cliPrinter.printError("Error selecting secondary address: " + e.getMessage(), false);
            }
            primaryAddress = 0xfd;
        }

        int length = dif.length + vif.length + data.length;
        byte[] dataRecord = ByteBuffer.allocate(length).put(dif).put(vif).put(data).array();
        try {
            mBusConnection.write(primaryAddress, dataRecord);
            cliPrinter.printInfo("Data was sent.");
        } catch (SerialPortTimeoutException e) {
            mBusConnection.close();
            cliPrinter.printError("Write attempt timed out.", false);
        } catch (IOException e) {
            mBusConnection.close();
            cliPrinter.printError("Error writing meter: " + e.getMessage(), false);
        }

        System.out.println();

        mBusConnection.close();
    }

    public static void read(ConsoleLineParser cliParser, MBusConnection mBusConnection, CliPrinter cliPrinter) {
        int primaryAddress = cliParser.getPrimaryAddress();
        SecondaryAddress secondaryAddress = cliParser.getSecondaryAddress();

        List<DataRecord> dataRecordsToSelectForReadout = new LinkedList<>();

        VerboseMessageListenerImpl messageListener = new VerboseMessageListenerImpl(cliPrinter);
        mBusConnection.setVerboseMessageListener(messageListener);

        VariableDataStructure variableDataStructure = null;
        if (secondaryAddress != null) {
            try {
                mBusConnection.selectComponent(secondaryAddress);
            } catch (SerialPortTimeoutException e) {
                mBusConnection.close();
                cliPrinter.printError("Selecting secondary address attempt timed out.", false);
            } catch (IOException e) {
                mBusConnection.close();
                cliPrinter.printError("Error selecting secondary address: " + e.getMessage(), false);
            }
            primaryAddress = 0xfd;
        }
        else {
            if (!cliParser.isLinkResetDisabled() && secondaryAddress == null) {
                try {
                    mBusConnection.linkReset(primaryAddress);
                } catch (InterruptedIOException e) {
                    mBusConnection.close();
                    cliPrinter.printError("Resetting link (SND_NKE) attempt timed out.", false);
                } catch (IOException e) {
                    mBusConnection.close();
                    cliPrinter.printError("Error resetting link (SND_NKE): " + e.getMessage(), false);
                }
                try {
                    Thread.sleep(100); // for slow slaves
                } catch (InterruptedException e) {
                    cliPrinter.printError("Thread sleep fails.\n" + e.getMessage(), false);
                }
            }
        }
        if (!dataRecordsToSelectForReadout.isEmpty()) {
            try {
                mBusConnection.selectForReadout(primaryAddress, dataRecordsToSelectForReadout);
            } catch (InterruptedIOException e) {
                mBusConnection.close();
                cliPrinter.printError("Selecting data record for readout timed out.", false);
            } catch (IOException e) {
                mBusConnection.close();
                cliPrinter.printError("Error selecting data record for readout: " + e.getMessage(), false);
            }
        }

        do {
            try {
                variableDataStructure = mBusConnection.read(primaryAddress);
            } catch (InterruptedIOException e) {
                mBusConnection.close();
                cliPrinter.printError("Read attempt timed out.", false);
            } catch (IOException e) {
                mBusConnection.close();
                cliPrinter.printError(e.getMessage(), false);
            }

            if (!dataRecordsToSelectForReadout.isEmpty()) {
                try {
                    mBusConnection.resetReadout(primaryAddress);
                } catch (InterruptedIOException e) {
                    cliPrinter.printError("Resetting meter for standard readout timed out.", false);
                } catch (IOException e) {
                    cliPrinter.printError("Error resetting meter for standard readout: " + e.getMessage(), false);
                }
            }

            cliPrinter.printInfo(variableDataStructure.toString());
            cliPrinter.printInfo();

        } while (variableDataStructure.moreRecordsFollow());

        mBusConnection.close();

    }

    public static void scan(String wildcardMask, boolean scanSecondaryAddress, MBusConnection mBusConnection,
            CliPrinter cliPrinter) throws IOException {

        try {
            mBusConnection.setVerboseMessageListener(new VerboseMessageListenerImpl(cliPrinter));

            cliPrinter.printInfo("Scanning address: ");

            if (scanSecondaryAddress) {
                mBusConnection.scan(wildcardMask, new SecondaryAddressListenerImpl());
            }
            else {
                scanPrimaryAddresses(mBusConnection, cliPrinter);
            }

        } finally {
            mBusConnection.close();
        }
        System.out.println("\nScan finished.");
    }

    private static void scanPrimaryAddresses(MBusConnection mBusConnection, CliPrinter cliPrinter) {
        for (int i = 0; i <= 250; i++) {
            if (i % 10 == 0 && i != 0) {
                cliPrinter.printlnInfo();
            }
            cliPrinter.printInfo(String.format("%3d%c", i, ','));
            try {
                mBusConnection.linkReset(i);
                try {
                    Thread.sleep(50); // for slow slaves
                } catch (InterruptedException e) {
                    cliPrinter.printError("\nThread sleep fails.\n" + e.getMessage(), false);
                }
                VariableDataStructure vdr = mBusConnection.read(i);
                cliPrinter.printInfo("\nFound device at primary address " + i + ":");
                cliPrinter.printlnInfo(vdr.getSecondaryAddress());

            } catch (InterruptedIOException e) {
                continue;
            } catch (IOException e) {
                cliPrinter.printlnInfo("\nError reading meter at primary address " + i + ": " + e.getMessage());
                continue;
            }
        }
    }

    static class SecondaryAddressListenerImpl implements SecondaryAddressListener {

        @Override
        public void newDeviceFound(SecondaryAddress secondaryAddress) {
            // do nothing, this application uses the return value of ScanSecondaryAddress.scan
        }

        @Override
        public void newScanMessage(String message) {
            System.out.println(message);
        }
    }

}
