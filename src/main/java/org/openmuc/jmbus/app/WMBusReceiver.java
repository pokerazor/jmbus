/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus.app;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.HexConverter;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.WMBusListener;
import org.openmuc.jmbus.WMBusMessage;
import org.openmuc.jmbus.WMBusMode;
import org.openmuc.jmbus.WMBusSap;
import org.openmuc.jmbus.WMBusSapAmber;
import org.openmuc.jmbus.WMBusSapImst;
import org.openmuc.jmbus.WMBusSapRadioCrafts;

class WMBusReceiver implements WMBusListener {

    private static boolean verboseMode = false;

    static void wmbus(ConsoleLineParser cliParser) {

        verboseMode = cliParser.isVerbose();
        WMBusMode mode = cliParser.getWMBusMode();
        String serialPortName = cliParser.getSerialPortName();
        String transceiverString = cliParser.getTransceiverString();

        WMBusSap tempMBusSap = null;
        if (transceiverString.equals("amber")) {
            tempMBusSap = new WMBusSapAmber(serialPortName, mode, new WMBusReceiver());
        }
        else if (transceiverString.equals("rc")) {
            tempMBusSap = new WMBusSapRadioCrafts(serialPortName, mode, new WMBusReceiver());
        }
        else if (transceiverString.equals("imst")) {
            tempMBusSap = new WMBusSapImst(serialPortName, mode, new WMBusReceiver());
        }
        else {
            cliParser.error("Not supported transceiver.", true);
        }

        final WMBusSap wMBusSap = tempMBusSap;

        Map<SecondaryAddress, byte[]> keyPairs = cliParser.getKeyPairs();
        Iterator<Entry<SecondaryAddress, byte[]>> iterator = keyPairs.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<SecondaryAddress, byte[]> entry = iterator.next();
            wMBusSap.setKey(entry.getKey(), entry.getValue());
        }

        try {
            wMBusSap.open();
        } catch (IOException e) {
            cliParser.error("Failed to open serial port: " + e.getMessage(), false);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (wMBusSap != null) {
                    wMBusSap.close();
                }
            }
        });

    }

    @Override
    public void newMessage(WMBusMessage message) {
        System.out.println("Message received at: " + new Date());

        try {
            message.decodeDeep();
        } catch (DecodingException e) {
            System.out.println("Unable to fully decode received message: " + e.getMessage());
            if (verboseMode) {
                System.out.println("Complete Message: " + HexConverter.toShortHexString(message.asBytes()));
                e.printStackTrace();
            }
        }

        System.out.println(message.toString());
        System.out.println();
    }

    @Override
    public void discardedBytes(byte[] bytes) {
        System.out.println("Bytes discarded: " + HexConverter.toShortHexString(bytes));
        System.out.println();
    }

    @Override
    public void stoppedListening(IOException e) {
        System.out.println("Stopped listening for new messages because: " + e.getMessage());
    }

}
