/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class MBusLPduTest {

    @Test
    public void testParser() throws IOException, DecodingException {
        byte[] msg = MessagesTest.testMsg1;

        MBusMessage mBusMessage = MBusMessage.decode(msg, msg.length);

        Assert.assertEquals(1, mBusMessage.getAddressField());

    }

    @Test
    public void testParser2() throws IOException, DecodingException {
        byte[] msg = MessagesTest.testMsg4;

        MBusMessage mBusMessage = MBusMessage.decode(msg, msg.length);

        Assert.assertEquals(0, mBusMessage.getAddressField());

    }
}
