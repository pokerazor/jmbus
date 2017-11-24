/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus.app;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.MBusSap;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jrxtx.SerialPortTimeoutException;

class ReadMeter {

    static void read(ConsoleLineParser cliParser) {

        int primaryAddress = cliParser.getPrimaryAddress();
        SecondaryAddress secondaryAddress = cliParser.getSecondaryAddress();

        List<DataRecord> dataRecordsToSelectForReadout = new ArrayList<>();

        MBusSap mBusSap = new MBusSap(cliParser.getSerialPortName(), cliParser.getBaudRate());
        mBusSap.setTimeout(cliParser.getTimeout());

        try {
            mBusSap.open();
        } catch (IOException e) {
            cliParser.error("Failed to open serial port: " + e.getMessage(), false);
        }

        if (cliParser.isVerbose()) {
            VerboseMessageListenerImpl messageListener = new VerboseMessageListenerImpl();
            mBusSap.setVerboseMessageListener(messageListener);
        }

        VariableDataStructure variableDataStructure = null;
        if (secondaryAddress != null) {
            try {
                mBusSap.selectComponent(secondaryAddress);
            } catch (SerialPortTimeoutException e) {
                mBusSap.close();
                cliParser.error("Selecting secondary address attempt timed out.", false);
            } catch (IOException e) {
                mBusSap.close();
                cliParser.error("Error selecting secondary address: " + e.getMessage(), false);
            }
            primaryAddress = 0xfd;
        }
        else {
            if (!cliParser.isLinkResetDisabled() && secondaryAddress == null) {
                try {
                    mBusSap.linkReset(primaryAddress);
                } catch (InterruptedIOException e) {
                    mBusSap.close();
                    cliParser.error("Resetting link (SND_NKE) attempt timed out.", false);
                } catch (IOException e) {
                    mBusSap.close();
                    cliParser.error("Error resetting link (SND_NKE): " + e.getMessage(), false);
                }
                try {
                    Thread.sleep(100); // for slow slaves
                } catch (InterruptedException e) {
                    cliParser.error("Thread sleep fails.\n" + e.getMessage(), false);
                }
            }
        }
        if (dataRecordsToSelectForReadout.size() > 0) {
            try {
                mBusSap.selectForReadout(primaryAddress, dataRecordsToSelectForReadout);
            } catch (SerialPortTimeoutException e) {
                mBusSap.close();
                cliParser.error("Selecting data record for readout timed out.", false);
            } catch (IOException e) {
                mBusSap.close();
                cliParser.error("Error selecting data record for readout: " + e.getMessage(), false);
            }
        }

        do {
            try {
                variableDataStructure = mBusSap.read(primaryAddress);
            } catch (InterruptedIOException e) {
                mBusSap.close();
                cliParser.error("Read attempt timed out.", false);
            } catch (IOException e) {
                mBusSap.close();
                cliParser.error("Error reading meter: " + e.getMessage(), false);
            }

            if (dataRecordsToSelectForReadout.size() > 0) {
                try {
                    mBusSap.resetReadout(primaryAddress);
                } catch (SerialPortTimeoutException e) {
                    System.err.println("Resetting meter for standard readout timed out.");
                } catch (IOException e) {
                    System.err.println("Error resetting meter for standard readout: " + e.getMessage());
                }
            }

            System.out.println(variableDataStructure.toString());
            System.out.println();

        } while (variableDataStructure.moreRecordsFollow());

        mBusSap.close();

    }

}
