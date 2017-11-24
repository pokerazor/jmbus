/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmuc.jmbus.internal.cli;

public class FlagCliParameter extends CliParameter {

    FlagCliParameter(CliParameterBuilder builder) {
        super(builder);
    }

    @Override
    int appendSynopsis(StringBuilder sb) {
        int length = 0;
        if (optional) {
            sb.append("[");
            length++;
        }
        sb.append(name);
        length += name.length();
        if (optional) {
            sb.append("]");
            length++;
        }
        return length;
    }

    @Override
    void appendDescription(StringBuilder sb) {
        sb.append("\t").append(name).append("\n\t    ").append(description);
    }

    @Override
    int parse(String[] args, int i) throws CliParseException {
        selected = true;
        return 1;
    }

}
