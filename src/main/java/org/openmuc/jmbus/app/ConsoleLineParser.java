/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openmuc.jmbus.HexConverter;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.WMBusMode;
import org.openmuc.jmbus.internal.cli.CliParameter;
import org.openmuc.jmbus.internal.cli.CliParameterBuilder;
import org.openmuc.jmbus.internal.cli.CliParseException;
import org.openmuc.jmbus.internal.cli.CliParser;
import org.openmuc.jmbus.internal.cli.FlagCliParameter;
import org.openmuc.jmbus.internal.cli.IntCliParameter;
import org.openmuc.jmbus.internal.cli.StringCliParameter;

class ConsoleLineParser {
    private final CliParser cliParser;

    private final static int WILDCARD_MASK_LENGTH = 8;

    private static SecondaryAddress secondaryAddressValue;
    private static int primaryAddressValue;
    private static byte[] difValue = {};
    private static byte[] vifValue = {};
    private static byte[] dataValue = {};
    private final String wildcardValue = "ffffffff";
    private WMBusMode wMBusModeValue;
    private final Map<SecondaryAddress, byte[]> keyPairValues = new TreeMap<>();

    private final StringCliParameter serialPort = new CliParameterBuilder("-sp")
            .setDescription(
                    "The serial port used for communication. Examples are /dev/ttyS0 (Linux) or COM1 (Windows).")
            .setMandatory()
            .buildStringParameter("serial_port");

    private final StringCliParameter address = new CliParameterBuilder("-a")
            .setDescription(
                    "The primary address of the meter. Primary addresses range from 0 to 255. Regular primary address range from 1 to 250. \n\t    Or the secondary address of the meter. Secondary addresses are 8 bytes long and shall be entered in hexadecimal form (e.g. 3a453b4f4f343423)")
            .setMandatory()
            .buildStringParameter("address");

    private final IntCliParameter baudRate = new CliParameterBuilder("-bd")
            .setDescription("Baud rate of the serial port.").buildIntParameter("baud_rate", 2400);

    private final IntCliParameter timeout = new CliParameterBuilder("-t")
            .setDescription("The timeout in milli seconds.").buildIntParameter("timeout", 3000);

    private final FlagCliParameter verbose = new CliParameterBuilder("-v")
            .setDescription("Enable verbose mode to print debug messages to standard out.").buildFlagParameter();

    private final FlagCliParameter disableLinkReset = new CliParameterBuilder("-dlr")
            .setDescription("Disable link reset in primary mode.").buildFlagParameter();

    private final FlagCliParameter secondaryScan = new CliParameterBuilder("-s").setDescription("Use secondary scan.")
            .buildFlagParameter();

    private final StringCliParameter wildcard = new CliParameterBuilder("-w")
            .setDescription("Use wildcard for region scan of secondary addresses e.g. 15ffffff")
            .buildStringParameter("wildcard");

    private final StringCliParameter dif = new CliParameterBuilder("-dif")
            .setDescription("The data information field. Minimal two hex signs length e.g. 01")
            .buildStringParameter("dif");

    private final StringCliParameter vif = new CliParameterBuilder("-vif")
            .setDescription("The value information field. Minimal two hex signs length e.g. 7a")
            .buildStringParameter("vif");

    private final StringCliParameter data = new CliParameterBuilder("-data")
            .setDescription("The date to write to the meter. Minimal two hex signs length. Only in mode (w)rite")
            .buildStringParameter("data");

    private final StringCliParameter transceiver = new CliParameterBuilder("-tr").setMandatory()
            .setDescription(
                    "The transceiver being used. It can be 'amber', 'imst' or 'rc' for modules from RadioCrafts.")
            .buildStringParameter("transceiver");

    private final StringCliParameter wmbusMode = new CliParameterBuilder("-wm").setMandatory()
            .setDescription("The wM-Bus mode can be S or T.")
            .buildStringParameter("wmbus_mode");

    private final StringCliParameter key = new CliParameterBuilder("-key")
            .setDescription(
                    "Address/key pairs that shall be used to decode the incoming messages. \n\t    The secondary address consists of 8 bytes that should be specified in hexadecimal form (1 byte are 2 hex signs). <address_1>:<key_2>;<adress_n+1>:<key_n+1>;...")
            .buildStringParameter("secondary_address_key_pair");

    ConsoleLineParser() {
        List<CliParameter> commonParams = new ArrayList<>();
        commonParams.add(serialPort);
        commonParams.add(timeout);
        commonParams.add(verbose);

        List<CliParameter> readParams = new ArrayList<>();
        readParams.addAll(commonParams);
        readParams.add(address);
        readParams.add(baudRate);
        readParams.add(disableLinkReset);

        List<CliParameter> writeParams = new ArrayList<>();
        writeParams.addAll(readParams);
        writeParams.add(dif);
        writeParams.add(vif);
        writeParams.add(data);

        List<CliParameter> scanParams = new ArrayList<>();
        scanParams.addAll(commonParams);
        scanParams.add(secondaryScan);
        scanParams.add(baudRate);

        List<CliParameter> wirelessParams = new ArrayList<>();
        wirelessParams.addAll(commonParams);
        wirelessParams.add(transceiver);
        wirelessParams.add(wmbusMode);
        wirelessParams.add(key);

        cliParser = new CliParser("jmbus-app", "jmbus master application to access meters wired and wireless");
        cliParser.addParameterGroup("read", readParams);
        cliParser.addParameterGroup("write", writeParams);
        cliParser.addParameterGroup("scan", scanParams);
        cliParser.addParameterGroup("wmbus", wirelessParams);
    }

    void start(String[] args) throws CliParseException {

        parse(args);

        switch (cliParser.getSelectedGroup().toLowerCase()) {
        case "read":
            ReadMeter.read(this);
            break;
        case "write":
            WriteMeter.write(this);
            break;
        case "scan":
            ScanForMeters.scan(this, secondaryScan.isSelected());
            break;
        case "wmbus":
            WMBusReceiver.wmbus(this);
            break;
        default:
            error("Unknown group: " + cliParser.getSelectedGroup().toLowerCase(), true);
        }

        if (wildcard.isSelected()) {
            checkWildcard();
        }

    }

    String getUsage() {
        return cliParser.getUsageString();
    }

    String getSerialPortName() {
        return serialPort.getValue();
    }

    int getBaudRate() {
        return baudRate.getValue();
    }

    int getPrimaryAddress() {
        return primaryAddressValue;
    }

    SecondaryAddress getSecondaryAddress() {
        return secondaryAddressValue;
    }

    byte[] getDif() {
        return difValue;
    }

    byte[] getVif() {
        return vifValue;
    }

    byte[] getData() {
        return dataValue;
    }

    int getTimeout() {
        return timeout.getValue();
    }

    String getWildcard() {
        return wildcardValue;
    }

    boolean isVerbose() {
        return verbose.isSelected();
    }

    boolean isLinkResetDisabled() {
        return disableLinkReset.isSelected();
    }

    WMBusMode getWMBusMode() {
        return wMBusModeValue;
    }

    String getTransceiverString() {
        return transceiver.getValue();
    }

    Map<SecondaryAddress, byte[]> getKeyPairs() {
        return keyPairValues;
    }

    void debug(Object... msg) {
        if (verbose.isSelected()) {
            StringBuilder sb = new StringBuilder();
            for (Object message : msg) {
                if (message.getClass().equals(byte[].class)) {
                    HexConverter.toHexString((byte[]) message);
                }
                sb.append(message);
            }
            System.out.println(sb.toString());
        }
    }

    void error(String errMsg, boolean printUsage) {
        System.err.println("Error: " + errMsg + "\n");
        if (printUsage) {
            System.out.println(getUsage());
        }
        System.exit(1);
    }

    private void parse(String[] args) throws CliParseException {
        cliParser.parseArguments(args);
        if (address.isSelected()) {
            parseAddress(address.getValue());
        }
        if (dif.isSelected() && !vif.isSelected() || !dif.isSelected() && vif.isSelected()) {
            error("Always both has to be selected dif and vif.", true);
        }
        if (dif.isSelected()) {
            difValue = convertDifVif(dif.getValue(), "dif");
        }
        if (vif.isSelected()) {
            vifValue = convertDifVif(vif.getValue(), "vif");
        }
        if (data.isSelected()) {
            dataValue = convertDifVif(data.getValue(), "data");
        }
        if (wmbusMode.isSelected()) {
            wMBusModeValue = parseWMBusMode();
        }
        if (key.isSelected()) {
            parseKeys();
        }
    }

    private void checkWildcard() {
        if (wildcardValue.length() != WILDCARD_MASK_LENGTH) {
            error("Allowed wildcard mask length is " + WILDCARD_MASK_LENGTH + " charactors but is "
                    + wildcardValue.length() + '.', false);
        }
    }

    private void parseAddress(String address) {
        long addrLength = address.length();

        if (addrLength > 3) {
            if (addrLength != 16) {
                error("The <secondary_address> has the wrong length. Should be 16 but is " + addrLength, true);
            }
            try {
                secondaryAddressValue = SecondaryAddress.getFromLongHeader(HexConverter.fromShortHexString(address), 0);
            } catch (NumberFormatException e) {
                error("The <secondary_address> parameter contains non hexadecimal character.", true);
            }
        }
        else {
            try {
                primaryAddressValue = Integer.parseInt(address);
            } catch (NumberFormatException e) {
                error("The <primary_address> parameter is not an integer value.", true);
            }
        }
    }

    private WMBusMode parseWMBusMode() {
        WMBusMode ret = null;
        try {
            ret = WMBusMode.valueOf(wmbusMode.getValue().toUpperCase());
        } catch (Exception e) {
            error("Unknown WMBus mode.", true);
        }
        return ret;
    }

    private void parseKeys() {
        String[] pairs = key.getValue().split(";");
        for (String pair : pairs) {
            String[] keyPair = pair.split(":");
            if (keyPair.length != 2) {
                error("A key has to be a secondary address and a key.", true);
            }
            else {
                int secondaryAddressLength = keyPair[0].length();
                if (secondaryAddressLength != 16) {
                    error("The secondary address needs 16 signs, but has " + secondaryAddressLength + '.', true);
                }
                else {
                    try {
                        byte[] secondaryAddressbytes = HexConverter.fromShortHexString(keyPair[0]);
                        SecondaryAddress secondaryAddress = SecondaryAddress
                                .getFromWMBusLinkLayerHeader(secondaryAddressbytes, 0);
                        try {
                            byte[] key = HexConverter.fromShortHexString(keyPair[1]);
                            keyPairValues.put(secondaryAddress, key);
                        } catch (NumberFormatException e) {
                            error("The key is not hexadecimal.", true);
                        }
                    } catch (NumberFormatException e) {
                        error("The secondary address is not hexadecimal.", true);
                    }

                }
            }
        }
    }

    private byte[] convertDifVif(String input, String inputName) {

        byte[] ret = {};

        if (input.length() < 1) {
            error("Minimal length of <" + inputName + "> is two hex signs.", true);
        }
        else {
            try {
                ret = HexConverter.fromShortHexString(input);
            } catch (NumberFormatException e) {
                error("The <" + inputName + "> parameter contains non hexadecimal character.", true);
            }
        }
        return ret;
    }

}
