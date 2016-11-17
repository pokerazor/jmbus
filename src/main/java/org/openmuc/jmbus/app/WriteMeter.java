package org.openmuc.jmbus.app;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import org.openmuc.jmbus.HexConverter;
import org.openmuc.jmbus.MBusSap;
import org.openmuc.jmbus.SecondaryAddress;

public class WriteMeter {

    private static void printUsage() {
        System.out.println(
                "SYNOPSIS\n\torg.openmuc.jmbus.app.WriteMeter <serial_port> (<primary_address> | <secondary_address>) <dif> <vif> <data_hex> [-b <baud_rate>] [-t <timeout>]");
        System.out.println(
                "DESCRIPTION\n\tWrites the given data to the given meter connected to the given serial port. Errors are printed to stderr.");
        System.out.println("OPTIONS");
        System.out.println(
                "\t<serial_port>\n\t    The serial port used for communication. Examples are /dev/ttyS0 (Linux) or COM1 (Windows)\n");
        System.out.println(
                "\t<primary_address>\n\t    The primary address of the meter. Primary addresses range from 0 to 255. Regular primary address range from 1 to 250.\n");
        System.out.println(
                "\t<secondary_address>\n\t    The secondary address of the meter. Secondary addresses are 8 bytes long and shall be entered in hexadecimal form (e.g. 3a453b4f4f343423)\n");
        System.out.println("\t<dif>\n\t    The data information field. Minimal two hex signs length e.g. 01\n");
        System.out.println("\t<vif>\n\t    The value information field. Minimal two hex signs length e.g. 7a\n");
        System.out.println("\t-b <baud_rate>\n\t    The baud rate used to connect to the meter. Default is 2400 bd.\n");
        System.out.println(
                "\t-t <timeout>\n\t    Timeout, in milli seconds, between send and receive acknowledge. Default is 500 ms\n");

        System.out.println();
        System.out.println("Example:");
        System.out.println(
                "\tChange primary address: org.openmuc.jmbus.app.WriteMeter /dev/ttyUSB0 <old_primary_address> 01 7a <data_new_primary_address>");
        System.out.println(
                "\tChange primary address from 20 to 26: org.openmuc.jmbus.app.WriteMeter /dev/ttyUSB0 20 01 7a 1a");
        System.out.println(
                "\tSet primary address with secondary address: org.openmuc.jmbus.app.WriteMeter /dev/ttyUSB0 <secondary_address> 01 7a <data_primary_address>");
        System.out.println(
                "\tSet primary address to 47 (0x2f) with secondary address 3a453b4f4f343423: org.openmuc.jmbus.app.WriteMeter /dev/ttyUSB0 3a453b4f4f343423 01 7a 2f");
    }

    public static void main(String[] args) {

        int baudRate = 2400;
        int timeout = 500;

        int minArgsLength = 5;

        int argsLength = args.length;
        if (argsLength < minArgsLength) {
            error("Error: too few arguments.", true);
        }

        String serialPortName = args[0];
        String address = args[1];
        String dif_string = args[2];
        String vif_string = args[3];
        String data_string = args[4];

        byte[] data = {};
        byte[] dif = {};
        byte[] vif = {};

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

        dif = convertInput(dif_string, "dif");
        vif = convertInput(vif_string, "vif");
        data = convertInput(data_string, "data");

        if (argsLength > minArgsLength) {
            int argsIndex = 5;
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
                else {
                    error("Error: unknown parameter.", true);
                }
            }
        }

        MBusSap mBusSap = new MBusSap(serialPortName, baudRate);
        mBusSap.setTimeout(timeout);
        try {
            mBusSap.open();
        } catch (IOException e2) {
            error("Failed to open serial port: " + e2.getMessage(), false);
        }

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
                System.out.println("Error by sending data.");
            }
        } catch (IOException e) {
            mBusSap.close();
            error("Error writing meter: " + e.getMessage(), false);
        } catch (TimeoutException e) {
            mBusSap.close();
            error("Write attempt timed out.", false);
        }

        System.out.println();

        mBusSap.close();
    }

    private static byte[] convertInput(String input, String inputName) {

        byte[] ret = {};

        if (input.length() < 1) {
            error("Error: minimal length of <" + inputName + "> is two hex signs.", true);
        }
        else {
            try {
                ret = HexConverter.fromShortHexString(input);
            } catch (NumberFormatException e) {
                error("Error: the <" + inputName + "> parameter contains non hexadecimal character.", true);
            }
        }
        return ret;
    }

    private static int parseIntegerArg(String[] args, int argsLength, int argsIndex) throws NumberFormatException {
        int ret = 0;
        if (argsLength < argsIndex + 2) {
            error("Missing value.", true);
        }
        try {
            ret = Integer.parseInt(args[++argsIndex]);
        } catch (NumberFormatException e) {
            error("Error: the <baud_rate> parameter is not an integer value.", false);
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
