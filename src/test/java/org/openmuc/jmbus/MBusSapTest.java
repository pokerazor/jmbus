/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

public class MBusSapTest {
    public void testResponseParser() {

    }

    @Test
    public void constructorTest() {
        @SuppressWarnings("resource")
        MBusSap mBusSap = new MBusSap("/dev/ttyS99", 2600);
        int timeout = 2000;
        mBusSap.setTimeout(timeout);
        Assert.assertTrue(mBusSap.getTimeout() == timeout);
    }

    @Test
    public void testParser2() throws IOException, DecodingException {
        testSingleMessage(MessagesTest.testMsg4, 0, 9, "TestParser2");
    }

    @Test
    public void testParser3() throws IOException, DecodingException {
        testSingleMessage(MessagesTest.testMsg5, 5, 10, "TestParser3");
    }

    @Test
    public void testParser4() throws IOException, DecodingException {
        testSingleMessage(MessagesTest.testMsg6, 13, 12, "TestParser4");
    }

    @Test
    public void testParser5() throws IOException, DecodingException {
        testSingleMessage(MessagesTest.testMsg7, 1, 12, "TestParser5");
    }

    @Test
    public void testParser6() throws IOException, DecodingException {
        testMultiMessages(MessagesTest.test_ABB_A41_messages, 9, MessagesTest.test_ABB_A41_DataRecodSizes, "ABB A41");
    }

    private void testSingleMessage(byte[] message, int expectedAddressField, int expectedDataRecodsSize,
            String deviceName) throws DecodingException, IOException {
        ArrayList<byte[]> messages = new ArrayList<>();
        messages.add(message);
        int[] dataRecodsSizes = { expectedDataRecodsSize };
        testMultiMessages(messages, expectedAddressField, dataRecodsSizes, deviceName);
    }

    private void testMultiMessages(ArrayList<byte[]> messages, int addressField, int[] dataRecodSizes,
            String deviceName) throws DecodingException, IOException {

        boolean moreMessages = true;
        int i = 0;
        byte[] msg;

        while (moreMessages && i <= messages.size() - 1) {
            msg = messages.get(i);
            MBusMessage lpdu = MBusMessage.decode(msg, msg.length);

            Assert.assertEquals(addressField, lpdu.getAddressField());

            VariableDataStructure vdr = lpdu.getVariableDataResponse();
            vdr.decode();
            Assert.assertEquals(dataRecodSizes[i], vdr.getDataRecords().size());

            moreMessages = vdr.moreRecordsFollow();
            ++i;
        }
    }
}
