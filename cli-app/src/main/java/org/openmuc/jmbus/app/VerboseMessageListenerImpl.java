/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.app;

import java.text.MessageFormat;

import javax.xml.bind.DatatypeConverter;

import org.openmuc.jmbus.VerboseMessage;
import org.openmuc.jmbus.VerboseMessageListener;

class VerboseMessageListenerImpl implements VerboseMessageListener {

    private final CliPrinter cliPrinter;

    public VerboseMessageListenerImpl(CliPrinter cliPrinter) {
        this.cliPrinter = cliPrinter;
        this.cliPrinter.printlnDebug("jmbus verbose mode is activated.");
    }

    @Override
    public void newVerboseMessage(VerboseMessage debugMessage) {
        String data = DatatypeConverter.printHexBinary(debugMessage.getMessage());
        String dir = debugMessage.getMessageDirection().toString().toLowerCase();
        String msg = MessageFormat.format("{0} message: {1}", dir, data);
        this.cliPrinter.printlnDebug("<verbose> %s </verbose>\n", msg);
    }

}
