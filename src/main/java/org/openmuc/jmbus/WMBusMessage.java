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
package org.openmuc.jmbus;

import java.util.HashMap;

/**
 * 
 * Represents a wireless M-Bus link layer message without the CRC checksum.
 * 
 * {@link WMBusMessage} is structured as follows:
 * <ul>
 * <li>Length (1 byte) - the length (number of bytes) of the complete message without the length byte and the CRC bytes.
 * </li>
 * <li>Control field (1 byte) - defines the frame type. 0x44 signifies an SND-NR (send no request) message that is sent
 * by meters in S1 mode.</li>
 * <li>Secondary address (8 bytes) - the secondary address consists of:
 * <ul>
 * <li>Manufacturer ID (2 bytes) -</li>
 * <li>Address (6 bytes) - consists of
 * <ul>
 * <li>Device ID (4 bytes) -</li>
 * <li>Version (1 byte) -</li>
 * <li>Device type (1 byte) -</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author Stefan Feuerhahn
 *
 */
public class WMBusMessage {

    private final byte[] buffer;
    private final Integer signalStrengthInDBm;
    HashMap<String, byte[]> keyMap;

    private int length;
    private int controlField;
    private SecondaryAddress secondaryAddress;
    private VariableDataStructure vdr;

    private boolean decoded = false;

    WMBusMessage(byte[] buffer, Integer signalStrengthInDBm, HashMap<String, byte[]> keyMap) {
        this.buffer = buffer;
        this.signalStrengthInDBm = signalStrengthInDBm;
        this.keyMap = keyMap;
    }

    public void decode() throws DecodingException {
        length = buffer[0] & 0xff;
        if (length > (buffer.length - 1)) {
            throw new DecodingException("byte buffer has only a length of " + buffer.length
                    + " while the specified length field is " + length);
        }
        controlField = buffer[1] & 0xff;
        secondaryAddress = SecondaryAddress.getFromWMBusLinkLayerHeader(buffer, 2);
        vdr = new VariableDataStructure(buffer, 10, length - 9, secondaryAddress, keyMap);

        decoded = true;
    }

    public void decodeDeep() throws DecodingException {
        decode();
        vdr.decode();
    }

    public boolean isDecoded() {
        return decoded;
    }

    public byte[] asBytes() {
        return buffer;
    }

    public int getControlField() {
        return controlField;
    }

    public SecondaryAddress getSecondaryAddress() {
        return secondaryAddress;
    }

    public VariableDataStructure getVariableDataResponse() {
        return vdr;
    }

    /**
     * Returns the received signal string indication (RSSI) in dBm.
     * 
     * @return the RSSI
     */
    public Integer getRssi() {
        return signalStrengthInDBm;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (signalStrengthInDBm != null) {
            builder.append("Message was received with signal strength: ").append(signalStrengthInDBm).append("dBm\n");
        }
        if (!decoded) {
            builder.append("Message has not been decoded. Bytes of this message:\n");
            HexConverter.appendHexString(builder, buffer, 0, buffer.length);
            return builder.toString();
        }
        else {
            builder.append("control field: ");
            HexConverter.appendHexString(controlField, builder);
            builder.append("\nSecondary Address -> ")
                    .append(secondaryAddress)
                    .append("\nVariable Data Response:\n")
                    .append(vdr);
            return builder.toString();
        }
    }

}
