/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus.app;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.openmuc.jmbus.MBusSap;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jrxtx.SerialPortTimeoutException;

class WriteMeter {

    private static void printExample() {
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

    static void write(ConsoleLineParser cliParser) {

        int primaryAddress = cliParser.getPrimaryAddress();
        SecondaryAddress secondaryAddress = cliParser.getSecondaryAddress();

        int baudRate = cliParser.getBaudRate();
        int timeout = cliParser.getTimeout();
        String serialPortName = cliParser.getSerialPortName();
        byte[] data = cliParser.getData();
        byte[] dif = cliParser.getDif();
        byte[] vif = cliParser.getVif();

        if (dif.length == 0 || vif.length == 0) {
            printExample();
            cliParser.error("No dif or vif setted.", true);
        }

        MBusSap mBusSap = new MBusSap(serialPortName, baudRate);
        mBusSap.setTimeout(timeout);

        try {
            mBusSap.open();
        } catch (IOException e2) {
            cliParser.error("Failed to open serial port: " + e2.getMessage(), false);
        }

        if (cliParser.isVerbose()) {
            VerboseMessageListenerImpl messageListener = new VerboseMessageListenerImpl();
            mBusSap.setVerboseMessageListener(messageListener);
        }

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

        byte[] dataRecord = ByteBuffer.allocate(dif.length + vif.length + data.length)
                .put(dif)
                .put(vif)
                .put(data)
                .array();
        try {
            if (mBusSap.write(primaryAddress, dataRecord)) {
                System.out.println("Data was sent.");
            }
            else {
                cliParser.error("By sending data.", false);
            }
        } catch (SerialPortTimeoutException e) {
            mBusSap.close();
            cliParser.error("Write attempt timed out.", false);
        } catch (IOException e) {
            mBusSap.close();
            cliParser.error("Error writing meter: " + e.getMessage(), false);
        }

        System.out.println();

        mBusSap.close();
    }

}
