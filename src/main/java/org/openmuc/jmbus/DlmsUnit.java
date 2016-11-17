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

import java.util.HashMap;
import java.util.Map;

/**
 * The units as defined in IEC 62056-6-2. Some units not defined in IEC 62056-6-2 but needed by M-Bus were added.
 * 
 */
public enum DlmsUnit {
    // can be found in IEC 62056-6-2 2013 Capture 5.2.2
    YEAR(1, "a"),
    MONTH(2, "mo"),
    WEEK(3, "wk"),
    DAY(4, "d"),
    HOUR(5, "h"),
    MIN(6, "min"),
    SECOND(7, "s"),
    DEGREE(8, "°"),
    DEGREE_CELSIUS(9, "°C"),
    CURRENCY(10, ""),
    METRE(11, "m"),
    METRE_PER_SECOND(12, "m/s"),
    CUBIC_METRE(13, "m³"),
    CUBIC_METRE_CORRECTED(14, "m³"),
    CUBIC_METRE_PER_HOUR(15, "m³/h"),
    CUBIC_METRE_PER_HOUR_CORRECTED(16, "m³/h"),
    CUBIC_METRE_PER_DAY(17, "m³/d"),
    CUBIC_METRE_PER_DAY_CORRECTED(18, "m³/d"),
    LITRE(19, "l"),
    KILOGRAM(20, "kg"),
    NEWTON(21, "N"),
    NEWTONMETER(22, "n"),
    PASCAL(23, "Nm"),
    BAR(24, "bar"),
    JOULE(25, "J"),
    JOULE_PER_HOUR(26, "J/h"),
    WATT(27, "W"),
    VOLT_AMPERE(28, "VA"),
    VAR(29, "var"),
    WATT_HOUR(30, "Wh"),
    VOLT_AMPERE_HOUR(31, "VAh"),
    VAR_HOUR(32, "varh"),
    AMPERE(33, "A"),
    COULOMB(34, "C"),
    VOLT(35, "V"),
    VOLT_PER_METRE(36, "V/m"),
    FARAD(37, "F"),
    OHM(38, "Ohm"),
    OHM_METRE(39, "Ohm m²/m"),
    WEBER(40, "Wb"),
    TESLA(41, "T"),
    AMPERE_PER_METRE(42, "A/m"),
    HENRY(43, "H"),
    HERTZ(44, "Hz"),
    ACTIVE_ENERGY_METER_CONSTANT_OR_PULSE_VALUE(45, "1/(Wh)"),
    REACTIVE_ENERGY_METER_CONSTANT_OR_PULSE_VALUE(46, "1/(varh)"),
    APPARENT_ENERGY_METER_CONSTANT_OR_PULSE_VALUE(47, "1(VAh)"),
    VOLT_SQUARED_HOURS(48, "V²h"),
    AMPERE_SQUARED_HOURS(49, "A²h"),
    KILOGRAM_PER_SECOND(50, "kg/s"),
    KELVIN(52, "S"),
    VOLT_SQUARED_HOUR_METER_CONSTANT_OR_PULSE_VALUE(53, "K"),
    AMPERE_SQUARED_HOUR_METER_CONSTANT_OR_PULSE_VALUE(54, "1/(V²h)"),
    METER_CONSTANT_OR_PULSE_VALUE(55, "1/(A²h)"),
    PERCENTAGE(56, "%"),
    AMPERE_HOUR(57, "Ah"),

    ENERGY_PER_VOLUME(60, "Wh/m³"),
    CALORIFIC_VALUE(61, "J/m³"),
    MOLE_PERCENT(62, "Mol %"),
    MASS_DENSITY(63, "g/m³"),
    PASCAL_SECOND(64, "Pa s"),
    SPECIFIC_ENERGY(65, "J/kg"),

    SIGNAL_STRENGTH(70, "dBm"),

    RESERVED(253, ""),
    OTHER_UNIT(254, ""),
    COUNT(255, ""),
    // not mentioned in 62056, added for MBus:
    CUBIC_METRE_PER_SECOND(150, "m³/s"),
    CUBIC_METRE_PER_MINUTE(151, "m³/min"),
    KILOGRAM_PER_HOUR(152, "kg/h"),
    CUBIC_FEET(153, "cft"),
    US_GALLON(154, "Impl. gal."),
    US_GALLON_PER_MINUTE(155, "Impl. gal./min"),
    US_GALLON_PER_HOUR(156, "Impl. gal./h"),
    DEGREE_FAHRENHEIT(157, "°F");

    private final int id;
    private final String unit;

    private static final Map<Integer, DlmsUnit> idMap = new HashMap<Integer, DlmsUnit>();

    static {
        for (DlmsUnit enumInstance : DlmsUnit.values()) {
            if (idMap.put(enumInstance.getId(), enumInstance) != null) {
                throw new IllegalArgumentException("duplicate ID: " + enumInstance.getId());
            }
        }
    }

    private DlmsUnit(int id, String unit) {
        this.id = id;
        this.unit = unit;
    }

    /**
     * Returns the ID of this DlmsUnit.
     * 
     * @return the ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the unit sign of this DlmsUnit.
     * 
     * @return the ID
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Returns the DlmsUnit that corresponds to the given ID. Returns DlmsUnit.RESERVED if no DlmsUnit with the given ID
     * exists.
     * 
     * @param id
     *            the ID
     * @return the DlmsUnit that corresponds to the given ID
     */
    public static DlmsUnit getInstance(int id) {
        DlmsUnit enumInstance = idMap.get(id);
        if (enumInstance == null) {
            enumInstance = DlmsUnit.RESERVED;
        }
        return enumInstance;
    }

}
