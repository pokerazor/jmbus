/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.wireless;

import java.io.IOException;
import java.text.MessageFormat;

import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.transportlayer.SerialBuilder;
import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.StopBits;

/**
 * A Wireless Mbus Connection.
 * 
 * @see #addKey(SecondaryAddress, byte[])
 */
public interface WMBusConnection extends AutoCloseable {

    /**
     * Closes the service access point.
     */
    @Override
    void close() throws IOException;

    /**
     * Stores a pair of secondary address and cryptographic key. The stored keys are automatically used to decrypt
     * messages when a wireless M-Bus message is been decoded.
     * 
     * @param address
     *            the secondary address.
     * @param key
     *            the cryptographic key.
     * 
     * @see #removeKey(SecondaryAddress)
     */
    void addKey(SecondaryAddress address, byte[] key);

    /**
     * Removes the stored key for the given secondary address.
     * 
     * @param address
     *            the secondary address for which to remove the stored key.
     * 
     * @see #addKey(SecondaryAddress, byte[])
     */
    void removeKey(SecondaryAddress address);

    class WMBusSerialBuilder extends SerialBuilder<WMBusConnection, WMBusSerialBuilder> {

        private WMBusManufacturer wmBusManufacturer;
        private WMBusMode mode;
        private WMBusListener listener;

        public WMBusSerialBuilder(WMBusManufacturer wmBusManufacturer, WMBusListener listener, String serialPortName) {
            super(serialPortName);
            this.listener = listener;
            this.wmBusManufacturer = wmBusManufacturer;
            this.mode = WMBusMode.T;

            switch (wmBusManufacturer) {
            case RADIO_CRAFTS:
                setBaudrate(19200);
                break;
            case AMBER:
                setBaudrate(9600);
                break;
            case IMST:
                setBaudrate(57600);
                break;
            default:
                // should not occur
                throw new RuntimeException(MessageFormat.format("Error unknown man {0}.", wmBusManufacturer));
            }
            setStopBits(StopBits.STOPBITS_1).setParity(Parity.NONE).setDataBits(DataBits.DATABITS_8);
        }

        public WMBusSerialBuilder setMode(WMBusMode mode) {
            this.mode = mode;

            return self();
        }

        public WMBusSerialBuilder setWmBusManufacturer(WMBusManufacturer wmBusManufacturer) {
            this.wmBusManufacturer = wmBusManufacturer;
            return self();
        }

        public WMBusSerialBuilder setListener(WMBusListener connectionListener) {
            this.listener = connectionListener;
            return self();
        }

        @Override
        public WMBusConnection build() throws IOException {
            AbstractWMBusConnection wmBusConnection;
            switch (this.wmBusManufacturer) {
            case AMBER:
                wmBusConnection = new WMBusConnectionAmber(mode, listener, buildTransportLayer());
                break;
            case IMST:
                wmBusConnection = new WMBusConnectionImst(mode, listener, buildTransportLayer());
                break;
            case RADIO_CRAFTS:
                wmBusConnection = new WMBusConnectionRadioCrafts(mode, listener, buildTransportLayer());
                break;
            default:
                // should not occur.
                throw new RuntimeException("Unknown Manufacturer.");
            }

            wmBusConnection.open();
            return wmBusConnection;
        }

        public enum WMBusManufacturer {
            AMBER,
            IMST,
            RADIO_CRAFTS
        }
    }

}
