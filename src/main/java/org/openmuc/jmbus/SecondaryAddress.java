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

import java.nio.ByteBuffer;
import java.util.Arrays;

public class SecondaryAddress {

    private String manufacturerId;
    private Bcd deviceId;
    private final int version;
    private final DeviceType deviceType;
    private final byte[] bytes = new byte[8];
    private final int hashCode;

    private SecondaryAddress(byte[] buffer, int offset, boolean longHeader) {
        System.arraycopy(buffer, offset, bytes, 0, bytes.length);
        hashCode = Arrays.hashCode(Arrays.copyOfRange(buffer, offset, bytes.length + offset - 1));

        int i = offset;

        if (longHeader) {
            i = decodeDeviceId(buffer, i);
            i = decodeManufacturerId(buffer, i);
        }
        else {
            i = decodeManufacturerId(buffer, i);
            i = decodeDeviceId(buffer, i);
        }
        version = buffer[i++] & 0xff;
        deviceType = DeviceType.getInstance(buffer[i++] & 0xff);
    }

    private int decodeManufacturerId(byte[] buffer, int i) {
        int manufacturerIdAsInt = (buffer[i++] & 0xff) + (buffer[i++] << 8);
        char c = (char) ((manufacturerIdAsInt & 0x1f) + 64);
        manufacturerIdAsInt = (manufacturerIdAsInt >> 5);
        char c1 = (char) ((manufacturerIdAsInt & 0x1f) + 64);
        manufacturerIdAsInt = (manufacturerIdAsInt >> 5);
        char c2 = (char) ((manufacturerIdAsInt & 0x1f) + 64);
        manufacturerId = "" + c2 + c1 + c;
        return i;
    }

    private static byte[] encodeManufacturerId(String manufactureId) {

        byte[] mfId = new byte[] { 0, 0 };

        if (manufactureId.length() == 3) {

            manufactureId = manufactureId.toUpperCase();

            char[] manufactureIdArray = manufactureId.toCharArray();
            int manufacturerIdAsInt = (manufactureIdArray[0] - 64) * 32 * 32;
            manufacturerIdAsInt += (manufactureIdArray[1] - 64) * 32;
            manufacturerIdAsInt += (manufactureIdArray[1] - 64);

            mfId = ByteBuffer.allocate(4).putInt(manufacturerIdAsInt).array();
        }

        return mfId;
    }

    private int decodeDeviceId(byte[] buffer, int i) {
        byte[] idArray = new byte[4];
        idArray[0] = buffer[i++];
        idArray[1] = buffer[i++];
        idArray[2] = buffer[i++];
        idArray[3] = buffer[i++];
        deviceId = new Bcd(idArray);
        return i;
    }

    public static SecondaryAddress getFromLongHeader(byte[] buffer, int offset) {
        return new SecondaryAddress(buffer, offset, true);
    }

    public static SecondaryAddress getFromWMBusLinkLayerHeader(byte[] buffer, int offset) {
        return new SecondaryAddress(buffer, offset, false);
    }

    public static SecondaryAddress getFromHexString(String hexString) throws NumberFormatException {
        byte[] buffer = HexConverter.fromShortHexString(hexString);
        return new SecondaryAddress(buffer, 0, true);
    }

    public static SecondaryAddress getFromManufactureId(byte[] idNumber, String manufactureId, byte version, byte media)
            throws NumberFormatException {

        if (idNumber.length == 8) {
            byte[] mfId = encodeManufacturerId(manufactureId);
            byte[] buffer = ByteBuffer.allocate(idNumber.length + mfId.length + 1 + 1)
                    .put(idNumber)
                    .put(mfId)
                    .put(version)
                    .put(media)
                    .array();
            return new SecondaryAddress(buffer, 0, true);
        }
        else {
            throw new NumberFormatException("Wrong length of idNumber. Allowed length is 8.");
        }
    }

    public byte[] asByteArray() {
        return bytes;
    }

    public int getHashCode() {
        return hashCode;
    }

    public String getManufacturerId() {
        return manufacturerId;
    }

    /**
     * Returns the device ID. This is secondary address of the device.
     * 
     * @return the device ID
     */
    public Bcd getDeviceId() {
        return deviceId;
    }

    /**
     * Returns the device type (e.g. gas, water etc.)
     * 
     * @return the device type
     */
    public DeviceType getDeviceType() {
        return deviceType;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("manufacturer ID: ")
                .append(manufacturerId)
                .append(", device ID: ")
                .append(deviceId)
                .append(", device version: ")
                .append(version)
                .append(", device type: ")
                .append(deviceType)
                .append(", as bytes: ");

        HexConverter.appendShortHexString(builder, bytes, 0, bytes.length);
        return builder.toString();
    }

}
