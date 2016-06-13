package org.openmuc.jmbus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ScanSecondaryAddress {

    private final static int MAX_LENGTH = 16;
    private static int pos = 0;
    private static byte[] value = new byte[MAX_LENGTH];

    private static List<SecondaryAddress> secondaryAddresses = new ArrayList<SecondaryAddress>();

    private ScanSecondaryAddress() {
    }

   
    /**
     *  * Scans for secondary addresses.
     * 
     * @param mBusSap the opened mBusSap
     * @param wildcardMask a wildcard mask for masking
     * @return a list of secondary addresses of all detected devices
     */
    public static List<SecondaryAddress> scan(MBusSap mBusSap, String wildcardMask) {

        boolean stop = false;
        boolean collision = false;

        wildcardMask = flipString(wildcardMask);
        wildcardMask += "ffffffff";

        for (int i = 0; i < MAX_LENGTH; ++i) {
            value[i] = Byte.parseByte(wildcardMask.substring(i, i + 1), 16);
        }

        pos = wildcardMask.indexOf("f");
        if (pos == 8 || pos < 0) {
            pos = 7;
        }
        value[pos] = 0;

        while (!stop) {

            System.out.println("scan with wildcard: " + toString(toSendByteArray(value)));
            SecondaryAddress secondaryAddessesWildCard = SecondaryAddress.getFromLongHeader(toSendByteArray(value), 0);
            SecondaryAddress readedSecondaryAddress = null;

            if (mBusSap.scanSelection(secondaryAddessesWildCard)) {

                try {
                    readedSecondaryAddress = mBusSap.read(0xfd).getSecondaryAddress();

                } catch (IOException e) {
                    System.out.println("Read (REQ_UD2) IOException / Collision");
                    collision = true;

                } catch (TimeoutException e) {
                    System.out.println("Read (REQ_UD2) TimeoutException");
                    collision = false;
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
                    if (readedSecondaryAddress != null) {
                        System.out.println("Detected Device:\n" + readedSecondaryAddress.toString());
                        secondaryAddresses.add(readedSecondaryAddress);
                        stop = handler();
                    }
                    else {
                        System.out.println("Problem to decode secondary address. Perhaps a collision.");
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
            value[i] = 15; // f
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

    private static String toString(byte[] value) {

        return HexConverter.toShortHexString(value);
    }
}
