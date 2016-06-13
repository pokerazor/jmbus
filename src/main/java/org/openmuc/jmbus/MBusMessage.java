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

/**
 * 
 * Represents a wired M-Bus link layer message according to EN 13757-2. The messages are in format class FT 1.2
 * according to IEC 60870-5-2.
 * 
 * If the M-Bus message is of frame type Long Frame it contains user data and it contains the following fields:
 * <ul>
 * <li>Length (1 byte) -</li>
 * <li>Control field (1 byte) -</li>
 * <li>Address field (1 byte) -</li>
 * <li>CI field (1 byte) -</li>
 * <li>The APDU (Variable Data Response) -</li>
 * </ul>
 * 
 */
class MBusMessage {

    enum MessageType {
        // the other message types (e.g. SND_NKE, REQ_UD2) cannot be sent from slave to master and are therefore
        // omitted.
        SINGLE_CHARACTER,
        RSP_UD;
    };

    private final MessageType messageType;
    private final int addressField;
    private final VariableDataStructure variableDataStructure;

    MBusMessage(byte[] buffer, int length) throws DecodingException {

        switch (buffer[0] & 0xff) {
        case 0xe5:
            messageType = MessageType.SINGLE_CHARACTER;
            addressField = 0;
            variableDataStructure = null;
            break;
        case 0x68:
            int lengthField = buffer[1] & 0xff;

            if (lengthField != length - 6) {
                throw new DecodingException(
                        "Wrong length field in frame header does not match the buffer length. Length field: "
                                + lengthField + ", buffer length: " + length + " !");
            }

            if (buffer[1] != buffer[2]) {
                throw new DecodingException("Length fields are not identical in long frame!");
            }

            if (buffer[3] != 0x68) {
                throw new DecodingException("Fourth byte of long frame was not 0x68.");
            }

            int controlField = buffer[4] & 0xff;

            if ((controlField & 0xcf) != 0x08) {
                throw new DecodingException(
                        "Unexptected control field value: " + HexConverter.toHexString((byte) controlField));
            }

            messageType = MessageType.RSP_UD;

            addressField = buffer[5] & 0xff;

            variableDataStructure = new VariableDataStructure(buffer, 6, length - 6, null, null);
            break;
        default:
            throw new DecodingException("Unexpected first frame byte: " + HexConverter.toHexString(buffer[0]));
        }
    }

    public int getAddressField() {
        return addressField;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public VariableDataStructure getVariableDataResponse() {
        return variableDataStructure;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("message type: ");
        builder.append(messageType);
        builder.append("\naddress field: ");
        builder.append(addressField & 0xff);
        builder.append("\nVariable Data Structure:\n").append(variableDataStructure);
        return builder.toString();
    }

}
