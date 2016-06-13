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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Representation of the data transmitted in RESP-UD (M-Bus) and SND-NR (wM-Bus) messages.
 * 
 * @author Stefan Feuerhahn
 * 
 */
public class VariableDataStructure {

    private final byte[] buffer;
    private final int offset;
    private final int length;
    private final SecondaryAddress linkLayerSecondaryAddress;
    HashMap<String, byte[]> keyMap;

    private SecondaryAddress secondaryAddress;
    private int accessNumber;
    private int status;
    private EncryptionMode encryptionMode;
    private int numberOfEncryptedBlocks;
    private byte[] manufacturerData = new byte[0];
    private byte[] encryptedVariableDataResponse = new byte[0];;
    private boolean moreRecordsFollow = false;

    private boolean decoded = false;

    private List<DataRecord> dataRecords;

    public VariableDataStructure(byte[] buffer, int offset, int length, SecondaryAddress linkLayerSecondaryAddress,
            HashMap<String, byte[]> keyMap) throws DecodingException {
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
        this.linkLayerSecondaryAddress = linkLayerSecondaryAddress;
        this.keyMap = keyMap;
    }

    public void decode() throws DecodingException {
        try {

            int ciField = buffer[offset] & 0xff;

            switch (ciField) {
            case 0x72:
                decodeLongHeader(buffer, offset + 1);
                decodeDataRecords(buffer, offset + 13, length - 13);
                break;
            case 0x78:
                decodeDataRecords(buffer, offset + 1, length - 1);
                break;
            case 0x7a:
                decodeShortHeader(buffer, offset + 1);
                if (encryptionMode == EncryptionMode.AES_CBC_IV) {
                    encryptedVariableDataResponse = new byte[length - 5];
                    System.arraycopy(buffer, offset + 5, encryptedVariableDataResponse, 0, length - 5);

                    byte[] key = keyMap.get(HexConverter.toShortHexString(linkLayerSecondaryAddress.asByteArray(), 0,
                            linkLayerSecondaryAddress.asByteArray().length));
                    if (key == null) {
                        throw new DecodingException(
                                "Unable to decode encrypted payload because no key for the following secondary address was registered: "
                                        + linkLayerSecondaryAddress);
                    }

                    decodeDataRecords(decryptMessage(key), 0, length - 5);
                    encryptedVariableDataResponse = null;
                }
                else if (encryptionMode == EncryptionMode.NONE) {
                    decodeDataRecords(buffer, offset + 5, length - 5);
                }
                else {
                    throw new DecodingException("Unsupported encryption mode used: " + encryptionMode);
                }
                break;
            default:
                if ((ciField >= 0xA0) && (ciField <= 0xB7)) {
                    throw new DecodingException(
                            "Manufacturer specific CI: " + HexConverter.toHexString((byte) ciField));
                }
                throw new DecodingException(
                        "Unable to decode message with this CI Field: " + HexConverter.toHexString((byte) ciField));
            }
        } catch (Exception e) {
            throw new DecodingException(e);
        }

        decoded = true;
    }

    public SecondaryAddress getSecondaryAddress() {
        return secondaryAddress;
    }

    public int getAccessNumber() {
        return accessNumber;
    }

    public EncryptionMode getEncryptionMode() {
        return encryptionMode;
    }

    public byte[] getManufacturerData() {
        return manufacturerData;
    }

    public int getNumberOfEncryptedBlocks() {
        return numberOfEncryptedBlocks;
    }

    public int getStatus() {
        return status;
    }

    public List<DataRecord> getDataRecords() {
        return dataRecords;
    }

    public boolean moreRecordsFollow() {
        return moreRecordsFollow;
    }

    private void decodeLongHeader(byte[] buffer, int offset) {

        secondaryAddress = SecondaryAddress.getFromLongHeader(buffer, offset);

        decodeShortHeader(buffer, offset + 8);

    }

    private void decodeShortHeader(byte[] buffer, int offset) {
        int i = offset;

        accessNumber = buffer[i++] & 0xff;
        status = buffer[i++] & 0xff;
        numberOfEncryptedBlocks = (buffer[i++] & 0xf0) >> 4;
        encryptionMode = EncryptionMode.getInstance(buffer[i++] & 0x0f);
    }

    private void decodeDataRecords(byte[] buffer, int offset, int length) throws DecodingException {

        dataRecords = new ArrayList<DataRecord>();

        int i = offset;

        while (i < offset + length - 2) {

            if ((buffer[i] & 0xef) == 0x0f) {
                // manufacturer specific data

                if ((buffer[i] & 0x10) == 0x10) {
                    moreRecordsFollow = true;
                }

                manufacturerData = Arrays.copyOfRange(buffer, i + 1, offset + length - 2);
                return;
            }

            if (buffer[i] == 0x2f) {
                // this is a fill byte because some encryption mechanisms need multiples of 8 bytes to encode data
                i++;
                continue;
            }

            DataRecord dataRecord = new DataRecord();
            i = dataRecord.decode(buffer, i, length);

            dataRecords.add(dataRecord);
        }

    }

    public byte[] decryptMessage(byte[] key) throws DecodingException {

        if (encryptionMode == EncryptionMode.NONE) {
            return encryptedVariableDataResponse;
        }

        if (encryptionMode != EncryptionMode.AES_CBC_IV) {
            throw new DecodingException("Unsupported encryption mode: " + encryptionMode);
        }

        if (key == null) {
            throw new DecodingException("No AES Key found for Device Address!");
        }

        AesCrypt tempcrypter = new AesCrypt(key, createInitializationVector(linkLayerSecondaryAddress));

        if (numberOfEncryptedBlocks * 16 > encryptedVariableDataResponse.length) {
            throw new DecodingException("Number of encrypted exceeds payload size!");
        }

        if (!tempcrypter.decrypt(encryptedVariableDataResponse, numberOfEncryptedBlocks * 16)) {
            throw new DecodingException("Decryption not successful!");
        }

        if (!(tempcrypter.getResult()[0] == 0x2f && tempcrypter.getResult()[1] == 0x2f)) {
            throw new DecodingException("Decryption unsuccessful! Wrong AES Key?");
        }

        System.arraycopy(tempcrypter.getResult(), 0, encryptedVariableDataResponse, 0, numberOfEncryptedBlocks * 16);

        return encryptedVariableDataResponse;
    }

    private byte[] createInitializationVector(SecondaryAddress linkLayerSecondaryAddress) {
        byte[] initializationVector = new byte[16];

        System.arraycopy(linkLayerSecondaryAddress.asByteArray(), 0, initializationVector, 0, 8);

        for (int i = 0; i < 8; i++) {
            initializationVector[8 + i] = (byte) accessNumber;
        }

        return initializationVector;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (!decoded) {
            builder.append("VariableDataResponse has not been decoded. Bytes:\n");
            HexConverter.appendHexString(builder, buffer, offset, length);
            return builder.toString();
        }
        else {

            if (secondaryAddress != null) {
                builder.append("Secondary address: {").append(secondaryAddress).append("}\n");
            }
            builder.append("Short Header: {Access No.: ")
                    .append(accessNumber)
                    .append(", status: ")
                    .append(status)
                    .append(", encryption mode: ")
                    .append(encryptionMode)
                    .append(", number of encrypted blocks: ")
                    .append(numberOfEncryptedBlocks)
                    .append("}");

            if (encryptedVariableDataResponse.length != 0) {
                builder.append("\nEncrypted variable data: " + HexConverter.toHexString(encryptedVariableDataResponse));
            }
            else {
                for (DataRecord dataRecord : dataRecords) {
                    builder.append("\n");
                    builder.append(dataRecord.toString());
                }
                if (manufacturerData.length != 0) {
                    builder.append("\n").append("Manufacturer specific bytes:\n").append(
                            HexConverter.toHexString(manufacturerData));
                }
                if (moreRecordsFollow) {
                    builder.append("\n").append("More records follow ...");
                }
            }
        }
        return builder.toString();

    }

}
