/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus.app;

import org.openmuc.jmbus.internal.cli.CliParseException;

public class JmbusApp {
    public static void main(String[] args) {
        ConsoleLineParser cliParser = new ConsoleLineParser();
        try {
            cliParser.start(args);
        } catch (CliParseException e) {
            cliParser.error(e.getMessage(), true);
        }
    }
}
