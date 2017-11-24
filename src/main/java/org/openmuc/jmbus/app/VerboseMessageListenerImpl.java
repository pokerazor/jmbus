/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus.app;

import org.openmuc.jmbus.HexConverter;
import org.openmuc.jmbus.VerboseMessage;
import org.openmuc.jmbus.VerboseMessageListener;

public class VerboseMessageListenerImpl implements VerboseMessageListener {

    VerboseMessageListenerImpl() {
        System.out.println("jmbus verbose mode is activated.");
    }

    @Override
    public void newVerboseMessage(VerboseMessage debugMessage) {
        System.out.println("<verbose> " + debugMessage.messageDirection().toString().toLowerCase() + " message: "
                + HexConverter.toShortHexString(debugMessage.message()) + " </verbose>");
    }

    @Override
    public void newVerboseMessage(String debugMessage) {
        System.out.println("<verbose>\n" + debugMessage + "\n</verbose>");
    }

}
