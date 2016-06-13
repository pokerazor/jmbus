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

import org.junit.Test;

public class MbusDemoMessageTest {

    @Test
    public void testQundis_WTT16_msg1() throws DecodingException {
        byte[] msgByte = new byte[] { (byte) 0x68, (byte) 0x40, (byte) 0x40, (byte) 0x68, (byte) 0x08, (byte) 0x00,
                (byte) 0x72, (byte) 0x71, (byte) 0x22, (byte) 0x23, (byte) 0x10, (byte) 0x65, (byte) 0x32, (byte) 0x18,
                (byte) 0x0E, (byte) 0x17, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0C, (byte) 0x22, (byte) 0x22,
                (byte) 0x37, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x6D, (byte) 0x30, (byte) 0x10, (byte) 0xDA,
                (byte) 0x19, (byte) 0x06, (byte) 0xFD, (byte) 0x0C, (byte) 0x18, (byte) 0x00, (byte) 0x0E, (byte) 0x00,
                (byte) 0x22, (byte) 0x03, (byte) 0x0D, (byte) 0xFD, (byte) 0x0B, (byte) 0x05, (byte) 0x36, (byte) 0x31,
                (byte) 0x54, (byte) 0x54, (byte) 0x57, (byte) 0x32, (byte) 0x6C, (byte) 0xFF, (byte) 0xFF, (byte) 0x02,
                (byte) 0xFA, (byte) 0x3D, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x7C, (byte) 0x06, (byte) 0x54,
                (byte) 0x54, (byte) 0x41, (byte) 0x42, (byte) 0x20, (byte) 0x25, (byte) 0x61, (byte) 0x43,
                (byte) 0x16 };
        new MBusMessage(msgByte, msgByte.length);
    }

    @Test
    public void testQundis_WTT16_msg2() throws DecodingException {
        byte[] msgByte = new byte[] { (byte) 0x68, (byte) 0x40, (byte) 0x40, (byte) 0x68, (byte) 0x08, (byte) 0x00,
                (byte) 0x72, (byte) 0x71, (byte) 0x22, (byte) 0x23, (byte) 0x10, (byte) 0x65, (byte) 0x32, (byte) 0x18,
                (byte) 0x0E, (byte) 0x1E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0C, (byte) 0x22, (byte) 0x23,
                (byte) 0x37, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x6D, (byte) 0x36, (byte) 0x11, (byte) 0xDA,
                (byte) 0x19, (byte) 0x06, (byte) 0xFD, (byte) 0x0C, (byte) 0x18, (byte) 0x00, (byte) 0x0E, (byte) 0x00,
                (byte) 0x22, (byte) 0x03, (byte) 0x0D, (byte) 0xFD, (byte) 0x0B, (byte) 0x05, (byte) 0x36, (byte) 0x31,
                (byte) 0x54, (byte) 0x54, (byte) 0x57, (byte) 0x32, (byte) 0x6C, (byte) 0xFF, (byte) 0xFF, (byte) 0x02,
                (byte) 0xFA, (byte) 0x3D, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x7C, (byte) 0x06, (byte) 0x54,
                (byte) 0x54, (byte) 0x41, (byte) 0x42, (byte) 0x20, (byte) 0x25, (byte) 0x61, (byte) 0x52,
                (byte) 0x16 };
        new MBusMessage(msgByte, msgByte.length);
    }

    @Test
    public void testQundis_WTT16_msg3() throws DecodingException {
        byte[] msgByte = new byte[] { (byte) 0x68, (byte) 0x40, (byte) 0x40, (byte) 0x68, (byte) 0x08, (byte) 0x00,
                (byte) 0x72, (byte) 0x71, (byte) 0x22, (byte) 0x23, (byte) 0x10, (byte) 0x65, (byte) 0x32, (byte) 0x18,
                (byte) 0x0E, (byte) 0x1F, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0C, (byte) 0x22, (byte) 0x23,
                (byte) 0x37, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x6D, (byte) 0x0D, (byte) 0x12, (byte) 0xDA,
                (byte) 0x19, (byte) 0x06, (byte) 0xFD, (byte) 0x0C, (byte) 0x18, (byte) 0x00, (byte) 0x0E, (byte) 0x00,
                (byte) 0x22, (byte) 0x03, (byte) 0x0D, (byte) 0xFD, (byte) 0x0B, (byte) 0x05, (byte) 0x36, (byte) 0x31,
                (byte) 0x54, (byte) 0x54, (byte) 0x57, (byte) 0x32, (byte) 0x6C, (byte) 0xFF, (byte) 0xFF, (byte) 0x02,
                (byte) 0xFA, (byte) 0x3D, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x7C, (byte) 0x06, (byte) 0x54,
                (byte) 0x54, (byte) 0x41, (byte) 0x42, (byte) 0x20, (byte) 0x25, (byte) 0x61, (byte) 0x2B,
                (byte) 0x16 };
        new MBusMessage(msgByte, msgByte.length);
    }
}
