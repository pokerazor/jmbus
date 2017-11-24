/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.app;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.bind.DatatypeConverter;

import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.internal.cli.CliParameter;
import org.openmuc.jmbus.internal.cli.CliParameterBuilder;
import org.openmuc.jmbus.internal.cli.CliParseException;
import org.openmuc.jmbus.internal.cli.CliParser;
import org.openmuc.jmbus.internal.cli.FlagCliParameter;
import org.openmuc.jmbus.internal.cli.IntCliParameter;
import org.openmuc.jmbus.internal.cli.StringCliParameter;
import org.openmuc.jmbus.transportlayer.Builder;
import org.openmuc.jmbus.transportlayer.SerialBuilder;
import org.openmuc.jmbus.wireless.WMBusConnection;
import org.openmuc.jmbus.wireless.WMBusConnection.WMBusSerialBuilder;
import org.openmuc.jmbus.wireless.WMBusConnection.WMBusSerialBuilder.WMBusManufacturer;
import org.openmuc.jmbus.wireless.WMBusMode;

class ConsoleLineParser {

    private final CliParser cliParser;

    private static final int WILDCARD_MASK_LENGTH = 8;

    private SecondaryAddress secondaryAddressValue;
    private int primaryAddressValue;
    private static byte[] difValue = {};
    private static byte[] vifValue = {};
    private static byte[] dataValue = {};
    private final String wildcardValue = "ffffffff";
    private WMBusMode wMBusModeValue;
    private final Map<SecondaryAddress, byte[]> keyPairValues = new TreeMap<>();

    private String hostAddress;
    private int port;

    private CommunicationPort comPortType = CommunicationPort.SERIAL;

    private enum CommunicationPort {
        SERIAL,
        TCP;
    }

    private final StringCliParameter comPort = new CliParameterBuilder("-cp").setDescription(
            "The serial port or TCP address and port used for communication. Examples Serial: /dev/ttyS0 (Linux) or COM1 (Windows). Example TCP: tcp:192.168.8.2:1084")
            .setMandatory()
            .buildStringParameter("communication_port");

    private final IntCliParameter baudRate = new CliParameterBuilder("-bd")
            .setDescription("Baud rate of the serial port.")
            .buildIntParameter("baud_rate");

    private final StringCliParameter address = new CliParameterBuilder("-a").setDescription(
            "The primary address of the meter. Primary addresses range from 0 to 255. Regular primary address range from 1 to 250. \n\t    Or the secondary address of the meter. Secondary addresses are 8 bytes long and shall be entered in hexadecimal form (e.g. 3a453b4f4f343423)")
            .setMandatory()
            .buildStringParameter("address");

    private final IntCliParameter timeout = new CliParameterBuilder("-t")
            .setDescription("The timeout in milli seconds.")
            .buildIntParameter("timeout", 3000);

    private final FlagCliParameter verbose = new CliParameterBuilder("-v")
            .setDescription("Enable verbose mode to print debug messages to standard out.")
            .buildFlagParameter();

    private final FlagCliParameter disableLinkReset = new CliParameterBuilder("-dlr")
            .setDescription("Disable link reset in primary mode.")
            .buildFlagParameter();

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

    private final StringCliParameter key = new CliParameterBuilder("-key").setDescription(
            "Address/key pairs that shall be used to decode the incoming messages. \n\t    The secondary address consists of 8 bytes that should be specified in hexadecimal form (1 byte are 2 hex signs). <address_1>:<key_2>;<adress_n+1>:<key_n+1>;...")
            .buildStringParameter("secondary_address_key_pair");

    private final CliPrinter cliPrinter;

    public ConsoleLineParser() {

        List<CliParameter> commonParams = new ArrayList<>();
        commonParams.add(comPort);
        commonParams.add(baudRate);
        commonParams.add(timeout);
        commonParams.add(verbose);

        List<CliParameter> readParams = new ArrayList<>();
        readParams.addAll(commonParams);
        readParams.add(address);
        readParams.add(disableLinkReset);

        List<CliParameter> writeParams = new ArrayList<>();
        writeParams.addAll(readParams);
        writeParams.add(dif);
        writeParams.add(vif);
        writeParams.add(data);

        List<CliParameter> scanParams = new ArrayList<>();
        scanParams.addAll(commonParams);
        scanParams.add(secondaryScan);

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

        this.cliPrinter = new CliPrinter(this.cliParser.getUsageString(), this.verbose);
    }

    public void start(String[] args) {

        Builder<?, ?> builder;
        try {
            parse(args);
            builder = newBuilder();
        } catch (CliParseException e) {
            this.cliPrinter.printError(e.getMessage(), true);
            return;
        }

        try {
            switch (cliParser.getSelectedGroup().toLowerCase()) {
            case "read":
                CliConnection.read(this, (MBusConnection) builder.build(), cliPrinter);
                break;
            case "write":
                CliConnection.write(this, (MBusConnection) builder.build(), cliPrinter);
                break;
            case "scan":
                CliConnection.scan(this.wildcard.getValue(), secondaryScan.isSelected(),
                        (MBusConnection) builder.build(), cliPrinter);
                break;
            case "wmbus":
                if (comPortType != CommunicationPort.SERIAL) {
                    this.cliPrinter.printError("Using wmbus with tcp is not possible, yet.", false);
                    break;
                }
                WMBusConnection wmBusConnection = (WMBusConnection) builder.build();
                Map<SecondaryAddress, byte[]> keyPairs = getKeyPairs();
                for (Entry<SecondaryAddress, byte[]> keyPair : keyPairs.entrySet()) {
                    wmBusConnection.addKey(keyPair.getKey(), keyPair.getValue());
                }

                WMBusStart.wmbus(wmBusConnection);
                break;
            default:
                this.cliPrinter.printError("Unknown group: " + cliParser.getSelectedGroup().toLowerCase(), true);
            }
        } catch (IOException e) {
            this.cliPrinter.printError(e.getMessage(), false);
        }

        if (wildcard.isSelected()) {
            checkWildcard();
        }

    }

    private Builder<?, ?> newBuilder() {
        switch (comPortType) {
        case TCP:
            return newTcpBuilder();

        case SERIAL:
        default:
            return newSerialBuilder();
        }
    }

    private Builder<?, ?> newTcpBuilder() {
        return MBusConnection.newTcpBuilder(getHostAddress(), getPort()).setTimeout(getTimeout());
    }

    private Builder<?, ?> newSerialBuilder() {
        SerialBuilder<?, ?> connectionBuilder;

        String cmmPort = this.comPort.getValue();
        if (cliParser.getSelectedGroup().equalsIgnoreCase("wmbus")) {

            WMBusManufacturer wmBusManufacturer = parseManufacturer();

            connectionBuilder = new WMBusSerialBuilder(wmBusManufacturer, new WMBusStart.WMBusReceiver(this.cliPrinter),
                    cmmPort).setMode(getWMBusMode());
        }
        else {
            connectionBuilder = MBusConnection.newSerialBuilder(cmmPort);
        }
        if (baudRate.isSelected()) {
            connectionBuilder.setBaudrate(getBaudRate());
        }

        return connectionBuilder.setTimeout(getTimeout());
    }

    private WMBusManufacturer parseManufacturer() {
        switch (getTransceiverString().toLowerCase()) {
        case "amber":
            return WMBusManufacturer.AMBER;
        case "rc":
            return WMBusManufacturer.RADIO_CRAFTS;
        case "imst":
            return WMBusManufacturer.IMST;
        default:
            this.cliPrinter.printError("Not supported transceiver.", true);
            throw new RuntimeException();
        }
    }

    public int getBaudRate() {
        return baudRate.getValue();
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public int getPort() {
        return port;
    }

    public SecondaryAddress getSecondaryAddress() {
        return secondaryAddressValue;
    }

    public int getPrimaryAddress() {
        return primaryAddressValue;
    }

    public byte[] getDif() {
        return difValue;
    }

    public byte[] getVif() {
        return vifValue;
    }

    public byte[] getData() {
        return dataValue;
    }

    public int getTimeout() {
        return timeout.getValue();
    }

    public String getWildcard() {
        return wildcardValue;
    }

    public boolean isVerbose() {
        return verbose.isSelected();
    }

    public boolean isLinkResetDisabled() {
        return disableLinkReset.isSelected();
    }

    public WMBusMode getWMBusMode() {
        return wMBusModeValue;
    }

    public String getTransceiverString() {
        return transceiver.getValue();
    }

    public Map<SecondaryAddress, byte[]> getKeyPairs() {
        return keyPairValues;
    }

    private void parse(String[] args) throws CliParseException {
        cliParser.parseArguments(args);
        if (address.isSelected()) {
            parseAddress(address.getValue());
        }
        if (dif.isSelected() && !vif.isSelected() || !dif.isSelected() && vif.isSelected()) {
            this.cliPrinter.printError("Always both has to be selected dif and vif.", true);
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

        String comPortValue = comPort.getValue().trim();

        if (comPortValue.startsWith("tcp:")) {
            comPortType = CommunicationPort.TCP;
            comPortValue = comPortValue.replace("tcp:", "");
            parseIpAddress(comPortValue);
        }
    }

    private void checkWildcard() {
        if (wildcardValue.length() != WILDCARD_MASK_LENGTH) {
            this.cliPrinter.printError("Allowed wildcard mask length is " + WILDCARD_MASK_LENGTH + " characters but is "
                    + wildcardValue.length() + '.', false);
        }
    }

    private void parseAddress(String address) {
        long addrLength = address.length();

        if (addrLength > 3) {
            if (addrLength != 16) {
                this.cliPrinter.printError(
                        "The <secondary_address> has the wrong length. Should be 16 but is " + addrLength, true);
            }
            try {
                secondaryAddressValue = SecondaryAddress.newFromLongHeader(DatatypeConverter.parseHexBinary(address),
                        0);
            } catch (NumberFormatException e) {
                this.cliPrinter.printError("The <secondary_address> parameter contains non hexadecimal character.",
                        true);
            }
        }
        else {
            try {
                primaryAddressValue = Integer.parseInt(address);
            } catch (NumberFormatException e) {
                this.cliPrinter.printError("The <primary_address> parameter is not an integer value.", true);
            }
        }
    }

    private WMBusMode parseWMBusMode() {
        WMBusMode ret = null;
        try {
            ret = WMBusMode.valueOf(wmbusMode.getValue().toUpperCase());
        } catch (Exception e) {
            this.cliPrinter.printError("Unknown WMBus mode.", true);
        }
        return ret;
    }

    private void parseIpAddress(String tcpAddress) {
        if (tcpAddress.contains(":")) {
            String[] ipAddressPort = tcpAddress.split(":");
            if (ipAddressPort.length == 2) {
                try {
                    hostAddress = ipAddressPort[0];
                    port = Integer.decode(ipAddressPort[1]);
                } catch (NumberFormatException e) {
                    this.cliPrinter.printError("Given TCP port is not a number", false);
                }
            }
            else {
                this.cliPrinter.printError("Address and port are needed for IP communication. eg: 127.0.0.1:1001",
                        true);
            }
        }
        else {
            this.cliPrinter.printError("No \":\" in host address:port given. eg: 127.0.0.1:1001", true);
        }
    }

    private void parseKeys() {
        String[] pairs = key.getValue().split(";");
        for (String pair : pairs) {
            String[] keyPair = pair.split(":");
            if (keyPair.length != 2) {
                this.cliPrinter.printError("A key has to be a secondary address and a key.", true);
            }
            else {
                int secondaryAddressLength = keyPair[0].length();
                if (secondaryAddressLength != 16) {
                    this.cliPrinter.printError(
                            "The secondary address needs 16 signs, but has " + secondaryAddressLength + '.', true);
                }
                else {
                    try {
                        byte[] secondaryAddressbytes = DatatypeConverter.parseHexBinary(keyPair[0]);
                        SecondaryAddress secondaryAddress = SecondaryAddress.newFromWMBusLlHeader(secondaryAddressbytes,
                                0);
                        try {
                            byte[] key = DatatypeConverter.parseHexBinary(keyPair[1]);
                            keyPairValues.put(secondaryAddress, key);
                        } catch (IllegalArgumentException e) {
                            this.cliPrinter.printError("The key is not hexadecimal.", true);
                        }
                    } catch (NumberFormatException e) {
                        this.cliPrinter.printError("The secondary address is not hexadecimal.", true);
                    }

                }
            }
        }
    }

    private byte[] convertDifVif(String input, String inputName) {
        byte[] ret = {};

        if (input.length() < 1) {
            this.cliPrinter.printError("Minimal length of <" + inputName + "> is two hex signs.", true);
        }
        else {
            try {
                ret = DatatypeConverter.parseHexBinary(input);
            } catch (IllegalArgumentException e) {
                String errMsg = MessageFormat.format("The <{0}> parameter contains non hexadecimal character.",
                        inputName);
                this.cliPrinter.printError(errMsg, true);
            }
        }
        return ret;
    }

}
