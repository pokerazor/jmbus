/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

import org.junit.Assert;
import org.junit.Test;

public class BcdTest {

    @Test
    public void testSetGetValue() {

        Bcd bcd = new Bcd(new byte[] { 0x44, 0x44, 0x44, 0x44 });

        Assert.assertEquals(44444444, bcd.intValue());
        Assert.assertEquals(44444444l, bcd.longValue());
        Assert.assertEquals("44444444", bcd.toString());

        bcd = new Bcd(new byte[] { 0x44, 0x44, 0x44, 0x04 });

        Assert.assertEquals(4444444, bcd.intValue());
        Assert.assertEquals(4444444l, bcd.longValue());
        Assert.assertEquals("04444444", bcd.toString());

        bcd = new Bcd(new byte[] { 0x44, 0x44, 0x44, (byte) 0xf4 });

        Assert.assertEquals(-4444444, bcd.intValue());
        Assert.assertEquals(-4444444l, bcd.longValue());
        Assert.assertEquals("-4444444", bcd.toString());

    }

}
