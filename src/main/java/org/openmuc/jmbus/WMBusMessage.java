/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.text.MessageFormat;
import java.util.Map;

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
    protected Map<SecondaryAddress, byte[]> keyMap;

    private int length;
    private int controlField;
    private SecondaryAddress secondaryAddress;
    private VariableDataStructure vdr;

    private boolean decoded = false;

    WMBusMessage(byte[] buffer, Integer signalStrengthInDBm, Map<SecondaryAddress, byte[]> keyMap) {
        this.buffer = buffer;
        this.signalStrengthInDBm = signalStrengthInDBm;
        this.keyMap = keyMap;
    }

    public void decode() throws DecodingException {
        length = buffer[0] & 0xff;
        if (length > (buffer.length - 1)) {
            String msg = MessageFormat.format(
                    "Byte buffer has only a length of {0} while the specified length field is {1}.", buffer.length,
                    length);
            throw new DecodingException(msg);
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
            return builder.append("\nSecondary Address -> ")
                    .append(secondaryAddress)
                    .append("\nVariable Data Response:\n")
                    .append(vdr)
                    .toString();
        }
    }

}
