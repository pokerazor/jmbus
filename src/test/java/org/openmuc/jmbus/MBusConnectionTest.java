/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmuc.jmbus.MBusConnection.MBusSerialBuilder;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class MBusConnectionTest {

    @Test
    @Ignore
    public void constructorTest() throws IOException {
        MBusSerialBuilder builder = MBusConnection.newSerialBuilder("/dev/ttyS99");
        MBusConnection mBusConnection = builder.build();
        mBusConnection.close();
    }

    public Object testParserData() {
        Object[] p1 = { MessagesData.testMsg4, 0, 9 };
        Object[] p2 = { MessagesData.testMsg5, 5, 10 };
        Object[] p3 = { MessagesData.testMsg6, 13, 12 };
        Object[] p4 = { MessagesData.testMsg7, 1, 12 };
        return new Object[] { p1, p2, p3, p4 };
    }

    @Test
    @Parameters(method = "testParserData")
    public void testParser(byte[] message, int expectedAddressField, int expectedDataRecodsSize)
            throws IOException, DecodingException {

        int[] dataRecodsSizes = { expectedDataRecodsSize };
        testMultiMessages(Arrays.asList(message), expectedAddressField, dataRecodsSizes);
    }

    @Test
    public void testParser6() throws IOException, DecodingException {
        testMultiMessages(MessagesData.test_ABB_A41_messages, 9, MessagesData.test_ABB_A41_DataRecodSizes);
    }

    private void testMultiMessages(List<byte[]> messages, int addressField, int[] dataRecodSizes)
            throws DecodingException, IOException {

        int i = 0;
        byte[] msg;

        while (i <= messages.size() - 1) {
            msg = messages.get(i);
            MBusMessage lpdu = MBusMessage.decode(msg, msg.length);

            assertEquals(addressField, lpdu.getAddressField());

            VariableDataStructure vdr = lpdu.getVariableDataResponse();
            vdr.decode();
            assertEquals(dataRecodSizes[i], vdr.getDataRecords().size());
            System.out.println(vdr.toString());
            if (!vdr.moreRecordsFollow()) {
                break;
            }

            ++i;
        }
    }
}
