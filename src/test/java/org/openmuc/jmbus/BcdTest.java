/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BcdTest {

    @Test
    public void test1() {
        Bcd bcd = new Bcd(new byte[] { 0x44, 0x44, 0x44, 0x44 });

        assertEquals(44444444, bcd.intValue());
        assertEquals(44444444l, bcd.longValue());
        assertEquals("44444444", bcd.toString());
    }

    @Test
    public void Test2() {
        Bcd bcd = new Bcd(new byte[] { 0x44, 0x44, 0x44, 0x04 });

        assertEquals(4444444, bcd.intValue());
        assertEquals(4444444l, bcd.longValue());
        assertEquals("04444444", bcd.toString());
    }

    @Test
    public void test3() {
        Bcd bcd = new Bcd(new byte[] { 0x44, 0x44, 0x44, (byte) 0xf4 });

        assertEquals(-4444444, bcd.intValue());
        assertEquals(-4444444l, bcd.longValue());
        assertEquals("-4444444", bcd.toString());

    }

}
