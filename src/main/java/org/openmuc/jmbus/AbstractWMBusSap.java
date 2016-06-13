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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;

abstract class AbstractWMBusSap implements WMBusSap {

    final static int BUFFER_LENGTH = 1000;
    final byte[] outputBuffer = new byte[BUFFER_LENGTH];
    final byte[] inputBuffer = new byte[BUFFER_LENGTH];

    final WMBusListener listener;
    final WMBusMode mode;

    SerialTransceiver serialTransceiver;

    final HashMap<String, byte[]> keyMap = new HashMap<String, byte[]>();
    volatile boolean closed = true;

    DataOutputStream os;
    DataInputStream is;

    AbstractWMBusSap(WMBusMode mode, WMBusListener listener) {
        this.listener = listener;
        this.mode = mode;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        serialTransceiver.close();
    }

    @Override
    public void setKey(SecondaryAddress address, byte[] key) {
        keyMap.put(HexConverter.toShortHexString(address.asByteArray()), key);
    }

    @Override
    public void removeKey(SecondaryAddress address) {
        keyMap.remove(HexConverter.toShortHexString(address.asByteArray()));
    }

}
