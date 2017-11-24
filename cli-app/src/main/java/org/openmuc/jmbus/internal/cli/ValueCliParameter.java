/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.internal.cli;

abstract class ValueCliParameter extends CliParameter {

    String parameterName;

    ValueCliParameter(CliParameterBuilder builder, String parameterName) {
        super(builder);
        this.parameterName = parameterName;
    }

    @Override
    int appendSynopsis(StringBuilder sb) {
        int length = 0;
        if (optional) {
            sb.append("[");
            length++;
        }
        sb.append(name).append(" <").append(parameterName).append(">");
        length += (name.length() + 3 + parameterName.length());
        if (optional) {
            sb.append("]");
            length++;
        }
        return length;
    }

    @Override
    void appendDescription(StringBuilder sb) {
        sb.append("\t").append(name).append(" <").append(parameterName).append(">\n\t    ").append(description);
    }
}
