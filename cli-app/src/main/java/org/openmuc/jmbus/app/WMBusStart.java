/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.app;

import static javax.xml.bind.DatatypeConverter.printHexBinary;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;

import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.wireless.WMBusConnection;
import org.openmuc.jmbus.wireless.WMBusListener;
import org.openmuc.jmbus.wireless.WMBusMessage;

class WMBusStart {

    public static void wmbus(final WMBusConnection wmBusConnection) throws IOException {

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (wmBusConnection == null) {
                    return;
                }
                try {
                    wmBusConnection.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        });

    }

    public static class WMBusReceiver implements WMBusListener {
        private CliPrinter cliPrinter;

        public WMBusReceiver(CliPrinter cliPrinter) {
            this.cliPrinter = cliPrinter;
        }

        @Override
        public void newMessage(WMBusMessage message) {
            this.cliPrinter.printlnInfo(MessageFormat.format("\n# {0} - new message: ", new Date()));

            try {
                message.getVariableDataResponse().decode();
            } catch (DecodingException e) {
                this.cliPrinter.printlnInfo("Unable to fully decode received message: \n" + e.getMessage());
                this.cliPrinter.printlnDebug("Complete Message:\n" + printHexBinary(message.asBlob()));
            }

            this.cliPrinter.printInfo(MessageFormat.format("{0}\n", message.toString()));
        }

        @Override
        public void discardedBytes(byte[] bytes) {
            this.cliPrinter.printlnInfo("Bytes discarded: " + printHexBinary(bytes));
            this.cliPrinter.printlnInfo();
        }

        @Override
        public void stoppedListening(IOException e) {
            this.cliPrinter.printlnInfo("Stopped listening for new messages because: " + e.getMessage());
        }

    }

}
