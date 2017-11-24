/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.openmuc.jrxtx.SerialPort;

abstract class AbstractWMBusSap implements WMBusSap {

    final static int BUFFER_LENGTH = 1000;
    final byte[] outputBuffer = new byte[BUFFER_LENGTH];
    final byte[] inputBuffer = new byte[BUFFER_LENGTH];

    final WMBusListener listener;
    final WMBusMode mode;

    SerialPort serialPort;

    final HashMap<SecondaryAddress, byte[]> keyMap = new HashMap<>();
    volatile boolean closed = true;

    DataOutputStream os;
    DataInputStream is;

    AbstractWMBusSap(WMBusMode mode, WMBusListener listener) {
        this.listener = listener;
        this.mode = mode;
    }

    @Override
    public void close() {
        if (closed || serialPort == null) {
            return;
        }

        try {
            serialPort.close();
            closed = true;
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void setKey(SecondaryAddress address, byte[] key) {
        keyMap.put(address, key);
    }

    @Override
    public void removeKey(SecondaryAddress address) {
        keyMap.remove(address);
    }

}
