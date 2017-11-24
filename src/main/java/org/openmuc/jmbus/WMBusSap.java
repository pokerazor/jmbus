/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.IOException;

public interface WMBusSap extends AutoCloseable {

    /**
     * Opens the serial port of this service access point and then configures the transceiver (e.g. sets the
     * transmission mode).
     * 
     * @throws IOException
     *             if any kind of error occurs while opening.
     */
    void open() throws IOException;

    /**
     * Closes the service access point and its associated serial port.
     */
    @Override
    void close();

    /**
     * Stores a pair of secondary address and cryptographic key. The stored keys are automatically used to decrypt
     * messages when {@link WMBusMessage#decode()} is called.
     * 
     * @param address
     *            the secondary address
     * @param key
     *            the cryptographic key
     */
    void setKey(SecondaryAddress address, byte[] key);

    /**
     * Removes the stored key for the given secondary address.
     * 
     * @param address
     *            the secondary address for which to remove the stored key
     */
    void removeKey(SecondaryAddress address);

}
