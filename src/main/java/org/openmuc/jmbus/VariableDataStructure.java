/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    private final Map<SecondaryAddress, byte[]> keyMap;

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
            Map<SecondaryAddress, byte[]> keyMap) throws DecodingException {
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
                if (encryptionMode == EncryptionMode.NONE) {
                    decodeDataRecords(buffer, offset + 5, length - 5);
                    break;
                }
                else if (encryptionMode != EncryptionMode.AES_CBC_IV) {
                    throw new DecodingException("Unsupported encryption mode used: " + encryptionMode);
                }

                decryptAesCbcIv();

                break;
            default:
                if ((ciField >= 0xA0) && (ciField <= 0xB7)) {
                    throw new DecodingException(
                            "Manufacturer specific CI: " + HexConverter.toHexString((byte) ciField));
                }
                throw new DecodingException(
                        "Unable to decode message with this CI Field: " + HexConverter.toHexString((byte) ciField));
            }
        } catch (DecodingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DecodingException(e);
        }

        decoded = true;
    }

    private void decryptAesCbcIv() throws DecodingException {
        encryptedVariableDataResponse = new byte[length - 5];
        System.arraycopy(buffer, offset + 5, encryptedVariableDataResponse, 0, length - 5);

        byte[] key = keyMap.get(linkLayerSecondaryAddress);

        if (key == null) {
            String msg = format("Unable to decode encrypted payload. \nSecondary address key was not registered: \n{0}",
                    linkLayerSecondaryAddress);
            throw new DecodingException(msg);
        }

        decodeDataRecords(decryptMessage(key), 0, length - 5);
        encryptedVariableDataResponse = null;
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

        dataRecords = new ArrayList<>();

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
            throw new DecodingException("AES key for give address not specified.");
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

        for (int i = 8; i < initializationVector.length; i++) {
            initializationVector[i] = (byte) accessNumber;
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

            if (encryptedVariableDataResponse != null && encryptedVariableDataResponse.length != 0) {
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
