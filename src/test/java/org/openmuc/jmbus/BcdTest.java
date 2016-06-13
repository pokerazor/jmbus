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

import org.junit.Assert;
import org.junit.Test;

public class BcdTest {

    @Test
    public void testSetGetValue() {

        Bcd myBcd = new Bcd(new byte[] { 0x44, 0x44, 0x44, 0x44 });

        System.out.println(Integer.MAX_VALUE);

        Assert.assertEquals(44444444, myBcd.intValue());

    }

}
