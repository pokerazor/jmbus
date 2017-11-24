/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.List;

public class ScanSecondaryAddress {

    private final static int MAX_LENGTH = 16;
    private static int pos = 0;
    private static byte[] value = new byte[MAX_LENGTH];

    /**
     * Scans for secondary addresses and returns all detected devices in a list and if SecondaryAddressListener not null
     * to the listen listener.
     * 
     * @param mBusSap
     *            the opened mBusSap
     * @param wildcardMask
     *            a wildcard mask for masking
     * @param secondaryAddressListener
     *            listener to get scan messages and scanned secondary address just at time.<br>
     *            If null, all detected address will only returned if finished.
     * 
     * @return a list of secondary addresses of all detected devices
     */
    public static List<SecondaryAddress> scan(MBusSap mBusSap, String wildcardMask,
            SecondaryAddressListener secondaryAddressListener) {

        boolean isListenerNull = secondaryAddressListener == null;
        List<SecondaryAddress> secondaryAddresses = new LinkedList<>();

        boolean stop = false;
        boolean collision = false;

        wildcardMask = flipString(wildcardMask);
        wildcardMask += "ffffffff";

        for (int i = 0; i < MAX_LENGTH; ++i) {
            value[i] = Byte.parseByte(wildcardMask.substring(i, i + 1), 16);
        }

        pos = wildcardMask.indexOf('f');
        if (pos == 8 || pos < 0) {
            pos = 7;
        }
        value[pos] = 0;

        while (!stop) {
            if (!isListenerNull && secondaryAddressListener.isScanMessageActive()) {
                sendMessageToListener(secondaryAddressListener,
                        "scan with wildcard: " + HexConverter.toShortHexString(toSendByteArray(value)));
            }
            SecondaryAddress secondaryAddessesWildCard = SecondaryAddress.getFromLongHeader(toSendByteArray(value), 0);
            SecondaryAddress readSecondaryAddress = null;

            if (mBusSap.scanSelection(secondaryAddessesWildCard)) {

                try {
                    readSecondaryAddress = mBusSap.read(0xfd).getSecondaryAddress();

                } catch (InterruptedIOException e) {
                    sendMessageToListener(secondaryAddressListener, "Read (REQ_UD2) TimeoutException");
                    collision = false;
                } catch (IOException e) {
                    sendMessageToListener(secondaryAddressListener, "Read (REQ_UD2) IOException / Collision");
                    collision = true;

                }
                if (collision) {
                    if (pos < 7) {
                        ++pos;
                        value[pos] = 0;
                    }
                    else {
                        stop = handler();
                    }
                    collision = false;
                }
                else {
                    if (readSecondaryAddress != null) {
                        if (!isListenerNull && secondaryAddressListener.isScanMessageActive()) {
                            sendMessageToListener(secondaryAddressListener,
                                    "Detected Device:\n" + readSecondaryAddress.toString());
                        }
                        secondaryAddresses.add(readSecondaryAddress);
                        if (!isListenerNull) {
                            secondaryAddressListener.newDeviceFound(readSecondaryAddress);
                        }
                        stop = handler();
                    }
                    else {
                        sendMessageToListener(secondaryAddressListener,
                                "Problem to decode secondary address. Perhaps a collision.");
                        if (pos < 7) {
                            ++pos;
                            value[pos] = 0;
                        }
                        else {
                            stop = handler();
                        }
                        collision = false;
                    }
                }
            }
            else {
                stop = handler();
            }
        }
        if (mBusSap != null) {
            mBusSap.close();
        }
        return secondaryAddresses;
    }

    private static boolean handler() {
        boolean stop;

        ++value[pos];

        if (value[pos] < 10) {
            stop = false;
        }
        else {
            if (pos > 0) {
                --pos;
                ++value[pos];
                setFValue();

                while (value[pos] > 10) {
                    --pos;
                    ++value[pos];
                    setFValue();
                }
                stop = false;
            }
            else {
                stop = true;
            }
        }

        return stop;
    }

    private static void setFValue() {
        for (int i = pos + 1; i < 8; ++i) {
            value[i] = 0xf;
        }
    }

    private static byte[] toSendByteArray(byte[] value) {
        byte[] sendByteArray = new byte[8];

        for (int i = 0; i < MAX_LENGTH; ++i) {

            if (i % 2 > 0) {
                sendByteArray[i / 2] |= value[i] << 4;
            }
            else {
                sendByteArray[i / 2] |= value[i];
            }
        }
        return sendByteArray;
    }

    /**
     * Flips character pairs. <br>
     * from 01253fffffffffff to 1052f3ffffffffff
     * 
     * @param value
     *            a string value like 01253fffffffffff
     * @return a fliped string value.
     */
    private static String flipString(String value) {
        StringBuilder flipped = new StringBuilder();

        for (int i = 0; i < value.length(); i += 2) {
            flipped.append(value.charAt(i + 1));
            flipped.append(value.charAt(i));
        }
        return flipped.toString();
    }

    private static void sendMessageToListener(SecondaryAddressListener secondaryAddressListener, String message) {
        if (secondaryAddressListener != null && secondaryAddressListener.isScanMessageActive()) {
            secondaryAddressListener.newScanMessage(message);
        }
    }

    /**
     * Don't let anyone instantiate this class.
     */
    private ScanSecondaryAddress() {
    }

}
