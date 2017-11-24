/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class MBusLPduTest {

    public Object testParserData() {
        Object[] p1 = { MessagesData.testMsg1, 1 };
        Object[] p2 = { MessagesData.testMsg4, 0 };
        return new Object[] { p1, p2 };
    }

    @Test
    @Parameters(method = "testParserData")
    public void testParser(byte[] msg, int addressField) throws IOException, DecodingException {

        MBusMessage mBusMessage = MBusMessage.decode(msg, msg.length);

        assertEquals(addressField, mBusMessage.getAddressField());
    }
}
