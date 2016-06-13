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

import java.io.IOException;

public interface WMBusSap {

    /**
     * Opens the serial port of this service access point and then configures the transceiver (e.g. sets the
     * transmission mode).
     * 
     * @throws IOException
     *             if any kind of error occurs while opening.
     */
    public void open() throws IOException;

    /**
     * Closes the service access point and its associated serial port.
     */
    public void close();

    /**
     * Stores a pair of secondary address and cryptographic key. The stored keys are automatically used to decrypt
     * messages when {@link WMBusMessage#decode()} is called.
     * 
     * @param address
     *            the secondary address
     * @param key
     *            the cryptographic key
     */
    public void setKey(SecondaryAddress address, byte[] key);

    /**
     * Removes the stored key for the given secondary address.
     * 
     * @param address
     *            the secondary address for which to remove the stored key
     */
    public void removeKey(SecondaryAddress address);

}
