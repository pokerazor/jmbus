/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.IOException;

/**
 * 
 * @author Stefan Feuerhahn
 *
 */
public interface WMBusListener {

    void newMessage(WMBusMessage message);

    void discardedBytes(byte[] bytes);

    void stoppedListening(IOException e);

}
