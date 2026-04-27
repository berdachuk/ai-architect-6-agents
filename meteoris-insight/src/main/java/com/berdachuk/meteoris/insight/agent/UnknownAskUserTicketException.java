package com.berdachuk.meteoris.insight.agent;

/**
 * Raised when the client posts an AskUser answer for a ticket id that is not pending.
 */
public class UnknownAskUserTicketException extends RuntimeException {

    private final String ticketId;

    public UnknownAskUserTicketException(String ticketId) {
        super("Unknown or expired AskUser ticket: " + ticketId);
        this.ticketId = ticketId;
    }

    public String ticketId() {
        return ticketId;
    }
}
