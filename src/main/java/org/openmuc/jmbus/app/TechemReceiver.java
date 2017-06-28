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

import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.HexConverter;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.TechemHKVMessage;
import org.openmuc.jmbus.WMBusMessage;
import org.openmuc.jmbus.WMBusMode;
import org.openmuc.jmbus.WMBusSap;
import org.openmuc.jmbus.WMBusSapAmber;
import org.openmuc.jmbus.WMBusSapRadioCrafts;

/**
 * 
 * @author 
 *
 */
public class TechemReceiver extends WMBusReceiver{
    private static boolean debugMode = false ;
    
	int[] filterIDs = {}; // put IDs of devices you are interested in. If emtpy, no filtering takes place.

	private static void printUsage() {
        System.out.println(
                "SYNOPSIS\n\torg.openmuc.jmbus.app.TechemReceiver <serial_port> <transceiver> <mode> [--debug] [<secondary_address>:<key>...]");
        System.out.println(
                "DESCRIPTION\n\tListens using a wireless M-Bus transceiver on the given serial port for proprietary Techem heat cost allocator wireless M-bus messages and prints them to stdout. Errors are printed to stderr.");
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
            printUsage();
            System.exit(1);
        }

        String serialPortName = args[0];

        String modeString = args[2].toUpperCase();
        WMBusMode mode = null;
        if (modeString.equals("S")) {
            mode = WMBusMode.S;
        }
        else if (modeString.equals("T")) {
            mode = WMBusMode.T;
        }
        else {
            printUsage();
            System.exit(1);
        }

        String transceiverString = args[1].toLowerCase();
        WMBusSap tempMBusSap = null;
        if (transceiverString.equals("amber")) {
            tempMBusSap = new WMBusSapAmber(serialPortName, mode, new TechemReceiver());
        }
        else if (transceiverString.equals("rc")) {
            tempMBusSap = new WMBusSapRadioCrafts(serialPortName, mode, new TechemReceiver());
        }
        else {
            printUsage();
            System.exit(1);
        }

        final WMBusSap wMBusSap = tempMBusSap;

        int startIndexOfKeys = 3;
        if (args.length > 3 && args[3].equals("--debug")) {
            debugMode  = true;
            startIndexOfKeys++;
        }

        for (int i = startIndexOfKeys; i < args.length; i++) {
            int index = args[i].indexOf(':');
            if (index == -1) {
                printUsage();
                System.exit(1);
            }
            wMBusSap.setKey(
                    SecondaryAddress.getFromWMBusLinkLayerHeader(
                            HexConverter.fromShortHexString(args[i].substring(0, index)), 0),
                    HexConverter.fromShortHexString(args[i].substring(index + 1)));
        }

        try {
            wMBusSap.open();
            System.out.println("Techem Listening started with device="+serialPortName+", Mode="+modeString+", transceiverType="+transceiverString);
        } catch (IOException e2) {
            System.err.println("Failed to open serial port: " + e2.getMessage());
            System.exit(1);
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
    
    boolean filterMatch(int inQuestion){
    	if(filterIDs.length==0){
    		return true;
    	}
    	for (int i = 0; i < filterIDs.length; i++) {
			if(filterIDs[i]==inQuestion){
				return true;
			}
		}
    	return false;
    }
	

    @Override
    public void newMessage(WMBusMessage message) {
        try {
            message.decodeDeep();
            if(filterMatch(message.getSecondaryAddress().getDeviceId().intValue())){
                System.out.println(message.toString());
            }
        } catch (DecodingException e) {
        	byte[] messageBytes=message.asBytes();
        	if ((messageBytes.length==51 || messageBytes.length==47) && (messageBytes[10] & 0xff) ==  0xa0 && message.getSecondaryAddress().getManufacturerId().equals("TCH")	){
        		newMessage(new TechemHKVMessage(message)); //standard a0
        	} else if ((messageBytes[10] & 0xff) ==  0xa2 && message.getSecondaryAddress().getManufacturerId().equals("TCH")){
        		newMessage(new TechemHKVMessage(message)); // at Karls'
        	} else if ((messageBytes[10] & 0xff) ==  0x80 && message.getSecondaryAddress().getManufacturerId().equals("TCH")){
        		newMessage(new TechemHKVMessage(message)); // at Karls' - warmwater?
        	} else {
                if (debugMode == true) {
                    System.out.println("TechemReceiver: Unable to fully decode received message: " + e.getMessage());
                    System.out.println("messageBytes.length="+messageBytes.length+" (messageBytes[10] & 0xff)="+(messageBytes[10] & 0xff)+" message.getSecondaryAddress().getManufacturerId()="+message.getSecondaryAddress().getManufacturerId());
               	    System.out.println(message.toString());
               		e.printStackTrace();
                }
        	}
        }
    }

    @Override
    public void discardedBytes(byte[] bytes) {
        System.out.println("Bytes discarded: " + HexConverter.toShortHexString(bytes));
    }

    @Override
    public void stoppedListening(IOException e) {
        System.out.println("Stopped listening for new messages because: " + e.getMessage());
    }
}