/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SecondaryAddress implements Comparable<SecondaryAddress> {

    private static final int SECONDARY_ADDRESS_LENGTH = 8;

    private final String manufacturerId;
    private final Bcd deviceId;
    private final int version;
    private final DeviceType deviceType;
    private final byte[] bytes;
    private final int hashCode;

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

        if (idNumber.length != SECONDARY_ADDRESS_LENGTH) {
            throw new NumberFormatException("Wrong length of ID. Length must be 8 byte.");
        }

        byte[] mfId = encodeManufacturerId(manufactureId);
        byte[] buffer = ByteBuffer.allocate(idNumber.length + mfId.length + 1 + 1)
                .put(idNumber)
                .put(mfId)
                .put(version)
                .put(media)
                .array();
        return new SecondaryAddress(buffer, 0, true);
    }

    public byte[] asByteArray() {
        return bytes;
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
        StringBuilder builder = new StringBuilder().append("manufacturer ID: ")
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

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SecondaryAddress)) {
            return false;
        }

        SecondaryAddress other = (SecondaryAddress) obj;

        return Arrays.equals(this.bytes, other.bytes);
    }

    @Override
    public int compareTo(SecondaryAddress sa) {
        return Integer.compare(hashCode(), sa.hashCode());
    }

    public static final int compare(SecondaryAddress a, SecondaryAddress b) {
        return a.compareTo(b);
    }

    private SecondaryAddress(byte[] buffer, int offset, boolean longHeader) {
        this.bytes = Arrays.copyOfRange(buffer, offset, offset + SECONDARY_ADDRESS_LENGTH);

        hashCode = Arrays.hashCode(this.bytes);

        try (ByteArrayInputStream is = new ByteArrayInputStream(this.bytes)) {
            if (longHeader) {
                this.deviceId = decodeDeviceId(is);
                this.manufacturerId = decodeManufacturerId(is);
            }
            else {
                this.manufacturerId = decodeManufacturerId(is);
                this.deviceId = decodeDeviceId(is);
            }
            version = is.read() & 0xff;
            deviceType = DeviceType.getInstance(is.read() & 0xff);
        } catch (IOException e) {
            // should not occur
            throw new RuntimeException(e);
        }
    }

    private static String decodeManufacturerId(ByteArrayInputStream is) {
        int manufacturerIdAsInt = (is.read() & 0xff) + (is.read() << 8);
        char c = (char) ((manufacturerIdAsInt & 0x1f) + 64);
        manufacturerIdAsInt = (manufacturerIdAsInt >> 5);
        char c1 = (char) ((manufacturerIdAsInt & 0x1f) + 64);
        manufacturerIdAsInt = (manufacturerIdAsInt >> 5);
        char c2 = (char) ((manufacturerIdAsInt & 0x1f) + 64);
        return "" + c2 + c1 + c;
    }

    private static byte[] encodeManufacturerId(String manufactureId) {

        if (manufactureId.length() != 3) {
            return new byte[] { 0, 0 };
        }

        manufactureId = manufactureId.toUpperCase();

        char[] manufactureIdArray = manufactureId.toCharArray();
        int manufacturerIdAsInt = (manufactureIdArray[0] - 64) * 32 * 32;
        manufacturerIdAsInt += (manufactureIdArray[1] - 64) * 32;
        manufacturerIdAsInt += (manufactureIdArray[1] - 64);

        return ByteBuffer.allocate(4).putInt(manufacturerIdAsInt).array();
    }

    private static Bcd decodeDeviceId(ByteArrayInputStream is) throws IOException {
        byte[] idArray = new byte[4];
        is.read(idArray);
        return new Bcd(idArray);
    }

}
