/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.wireless;

/**
 * The wireless M-Bus modes.
 */
public enum WMBusMode {
    /**
     * Frequent (868 MHz). Meter sends data several times/day.
     */
    T(0x02),
    /**
     * Stationary (8268 MHz). Meter sends data few times/day.
     */
    S(0x00);

    private int flag;

    private WMBusMode(int flag) {
        this.flag = flag;
    }

    public int getFlag() {
        return this.flag;
    }

}
