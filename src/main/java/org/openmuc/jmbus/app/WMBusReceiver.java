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
import java.util.Date;
import java.util.Locale;

import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.HexConverter;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.WMBusListener;
import org.openmuc.jmbus.WMBusMessage;
import org.openmuc.jmbus.WMBusMode;
import org.openmuc.jmbus.WMBusSap;
import org.openmuc.jmbus.WMBusSapAmber;
import org.openmuc.jmbus.WMBusSapRadioCrafts;

/**
 * 
 * @author Stefan Feuerhahn
 *
 */
public class WMBusReceiver implements WMBusListener {

    private static boolean debugMode = false;

    private static void printUsage() {
        System.out.println(
                "SYNOPSIS\n\torg.openmuc.jmbus.app.WMBusReceiver <serial_port> <transceiver> <mode> [--debug] [<secondary_address>:<key>...]");
        System.out.println(
                "DESCRIPTION\n\tListens using a wireless M-Bus transceiver on the given serial port for wireless M-bus messages and prints them to stdout. Errors are printed to stderr.");
        System.out.println("OPTIONS");
        System.out.println(
                "\t<serial_port>\n\t    The serial port used for communication. Examples are /dev/ttyS0 (Linux) or COM1 (Windows)\n");
        System.out.println(
                "\t<transceiver>\n\t    The transceiver being used. It can be 'amber' or 'rc' for modules from RadioCrafts\n");
        System.out.println("\t<mode>\n\t    The wM-Bus mode can be S or T\n");
        System.out.println("\t--debug\n\t    Print more verbose error information\n");
        System.out.println(
                "\t<secondary_address>:<key>...\n\t    Address/key pairs that shall be used to decode the incoming messages. The secondary address consists of 8 bytes that should be specified in hexadecimal form.\n");

    }

    public static void main(String[] args) {
        if (args.length < 3) {
            error("Error: too few arguments.", true);
        }

        String serialPortName = args[0];

        String modeString = args[2].toUpperCase(Locale.US);
        WMBusMode mode = null;
        if (modeString.equals("S")) {
            mode = WMBusMode.S;
        }
        else if (modeString.equals("T")) {
            mode = WMBusMode.T;
        }
        else {
            error("Error: unknown argument.", true);
        }

        String transceiverString = args[1].toLowerCase();
        WMBusSap tempMBusSap = null;
        if (transceiverString.equals("amber")) {
            tempMBusSap = new WMBusSapAmber(serialPortName, mode, new WMBusReceiver());
        }
        else if (transceiverString.equals("rc")) {
            tempMBusSap = new WMBusSapRadioCrafts(serialPortName, mode, new WMBusReceiver());
        }
        else {
            error("Error: not supported transceiver.", true);
        }

        final WMBusSap wMBusSap = tempMBusSap;

        int startIndexOfKeys = 3;
        if (args.length > 3 && args[3].equals("--debug")) {
            debugMode = true;
            startIndexOfKeys++;
        }

        for (int i = startIndexOfKeys; i < args.length; i++) {
            int index = args[i].indexOf(':');
            if (index == -1) {
                error("Error: wrong syntax for secondary address key pairs", true);
            }
            wMBusSap.setKey(
                    SecondaryAddress.getFromWMBusLinkLayerHeader(
                            HexConverter.fromShortHexString(args[i].substring(0, index)), 0),
                    HexConverter.fromShortHexString(args[i].substring(index + 1)));
        }

        try {
            wMBusSap.open();
        } catch (IOException e2) {
            error("Failed to open serial port: " + e2.getMessage(), false);
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
            if (debugMode == true) {
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

    private static void error(String errMsg, boolean printUsage) {
        System.err.println(errMsg + "\n");
        if (printUsage) {
            printUsage();
        }
        System.exit(1);
    }

}
