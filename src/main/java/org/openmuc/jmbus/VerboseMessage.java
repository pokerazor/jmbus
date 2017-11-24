/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.openmuc.jmbus;

public class VerboseMessage {

    private final MessageDirection messageDirection;
    private final byte[] message;

    public VerboseMessage(MessageDirection messageDirection, byte[] message) {
        this.messageDirection = messageDirection;
        this.message = message;
    }

    public byte[] message() {
        return message;
    }

    public MessageDirection messageDirection() {
        return messageDirection;
    }

    public enum MessageDirection {
        SEND,
        RECEIVE;
    }

}
