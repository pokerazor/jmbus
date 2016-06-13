/*
 * Copyright 2010-16 Fraunhofer ISE
 *
 * This file is part of jMBus.
 * For more information visit http://www.openmuc.org
 *
 * jMBus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jMBus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jMBus.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.jmbus;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class MBusSapTest {
    public void testResponseParser() {

    }

    @Test
    public void constructorTest() {
        MBusSap mBusSap = new MBusSap("/dev/ttyS99", 2600);
        int timeout = 2000;
        mBusSap.setTimeout(timeout);
        Assert.assertTrue(mBusSap.getTimeout() == timeout);

    }

    @Test
    public void testParser2() throws IOException, DecodingException {
        byte[] msg = MessagesTest.testMsg4;

        MBusMessage mBusMessage = new MBusMessage(msg, msg.length);

        Assert.assertEquals(0, mBusMessage.getAddressField());

        VariableDataStructure vdr = mBusMessage.getVariableDataResponse();
        vdr.decode();

        Assert.assertEquals(9, vdr.getDataRecords().size());

        System.out.println("\nTestParser2\n" + vdr.toString());
    }

    @Test
    public void testParser3() throws IOException, DecodingException {
        byte[] msg = MessagesTest.testMsg5;

        MBusMessage lpdu = new MBusMessage(msg, msg.length);

        Assert.assertEquals(5, lpdu.getAddressField());

        VariableDataStructure vds = lpdu.getVariableDataResponse();
        vds.decode();

        Assert.assertEquals(10, vds.getDataRecords().size());

        System.out.println("\nTestParser3\n" + vds.toString());
    }

    @Test
    public void testParser4() throws IOException, DecodingException {
        byte[] msg = MessagesTest.testMsg6;

        MBusMessage lpdu = new MBusMessage(msg, msg.length);

        Assert.assertEquals(13, lpdu.getAddressField());

        VariableDataStructure vdr = lpdu.getVariableDataResponse();
        vdr.decode();

        Assert.assertEquals(12, vdr.getDataRecords().size());

        System.out.println("\nTestParser4\n" + vdr.toString());
    }

    @Test
    public void testParser5() throws IOException, DecodingException {
        byte[] msg = MessagesTest.testMsg7;

        MBusMessage lpdu = new MBusMessage(msg, msg.length);

        Assert.assertEquals(1, lpdu.getAddressField());

        VariableDataStructure vdr = lpdu.getVariableDataResponse();
        vdr.decode();

        Assert.assertEquals(12, vdr.getDataRecords().size());

        System.out.println("\nTestParser5\n" + vdr.toString());
    }

    @Test
    public void testParser6() throws IOException, DecodingException {

        boolean moreMessages = true;
        int i = 0;
        byte[] msg;

        while (moreMessages) {
            msg = MessagesTest.test_ABB_A41_messages.get(i);
            MBusMessage lpdu = new MBusMessage(msg, msg.length);

            Assert.assertEquals(9, lpdu.getAddressField());

            VariableDataStructure vdr = lpdu.getVariableDataResponse();
            vdr.decode();
            System.out.println("\nABB A41 Message No. " + (i + 1) + "\n" + vdr.toString());
            moreMessages = vdr.moreRecordsFollow();
            ++i;
        }

    }
}
