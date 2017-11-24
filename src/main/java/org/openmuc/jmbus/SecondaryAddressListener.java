/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

/**
 * Listener to get secondarsy address scan message e.g. for console tools and to get messages.
 */
public abstract class SecondaryAddressListener {

    private final boolean isScanMessageActive;

    /**
     * For activating scan messages, set isScanMessageActive to true
     * 
     * @param isScanMessageActive
     *            true for activating scan messages
     */
    public SecondaryAddressListener(boolean isScanMessageActive) {
        this.isScanMessageActive = isScanMessageActive;
    }

    /**
     * Only active if isScanMessageActive is true. <br>
     * 
     * 
     * @param message
     *            messages from scan secondary address
     */
    public abstract void newScanMessage(String message);

    /**
     * 
     * @param secondaryAddress
     *            secondary address of detected device
     */
    public abstract void newDeviceFound(SecondaryAddress secondaryAddress);

    public boolean isScanMessageActive() {
        return isScanMessageActive;
    }
}
