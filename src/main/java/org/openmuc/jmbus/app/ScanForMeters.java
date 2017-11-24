/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus.app;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.openmuc.jmbus.MBusSap;
import org.openmuc.jmbus.ScanSecondaryAddress;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.SecondaryAddressListener;

class ScanForMeters {

    static void scan(ConsoleLineParser cliParser, boolean scanSecondaryAddress) {

        int baudRate = cliParser.getBaudRate();
        int timeout = cliParser.getTimeout();
        String wildcardMask = cliParser.getWildcard();
        String serialPortName = cliParser.getSerialPortName();

        MBusSap mBusSap = new MBusSap(serialPortName, baudRate);
        mBusSap.setTimeout(timeout);

        try {
            mBusSap.setTimeout(timeout);
            mBusSap.open();

            if (cliParser.isVerbose()) {
                VerboseMessageListenerImpl messageListener = new VerboseMessageListenerImpl();
                mBusSap.setVerboseMessageListener(messageListener);
            }

            System.out.println("Scanning address: ");

            if (scanSecondaryAddress) {
                ScanSecondaryAddress.scan(mBusSap, wildcardMask, new ScanForMeters.SecondaryAddressListenerImpl(true));
            }
            else {
                scanPrimaryAddresses(mBusSap, cliParser);
            }

        } catch (IOException e2) {
            System.out.println("Failed to open serial port: " + e2.getMessage());
            return;
        } finally {
            mBusSap.close();
        }
        System.out.println();
        System.out.println("Scan finished.");

    }

    private static void scanPrimaryAddresses(MBusSap mBusSap, ConsoleLineParser cliParser) {
        System.out.print("\nScanning address:");
        for (int i = 0; i <= 250; i++) {

            if (i % 10 == 0) {
                System.out.println();
            }
            System.out.printf("%3d%c", i, ',');
            try {
                mBusSap.linkReset(i);
                try {
                    Thread.sleep(50); // for slow slaves
                } catch (InterruptedException e) {
                    cliParser.error("\nThread sleep fails.\n" + e.getMessage(), false);
                }
                mBusSap.read(i);
                System.out.println();
                System.out.println("\nFound device at primary address " + i + ".");

            } catch (InterruptedIOException e) {
                continue;
            } catch (IOException e) {
                System.out.println();
                System.out.println("\nError reading meter at primary address " + i + ": " + e.getMessage());
                continue;
            }
        }
    }

    static class SecondaryAddressListenerImpl extends SecondaryAddressListener {

        SecondaryAddressListenerImpl(boolean isScanMessageActive) {
            super(isScanMessageActive);
        }

        @Override
        public void newDeviceFound(SecondaryAddress secondaryAddress) {
            // do nothing, this application uses the return value of ScanSecondaryAddress.scan
        }

        @Override
        public void newScanMessage(String message) {
            System.out.println(message);
        }
    }

}
