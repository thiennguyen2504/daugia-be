package com.example.daugia.common.logging;

/**
 * Canonical MDC / structured-log field names.
 * Use these constants as keys everywhere — never raw strings.
 * Ensures consistent field naming across all log sinks (file, ELK, etc.).
 */
public enum LogField {
    TRACE_ID("traceId"),
    ACTOR("actor"),
    USER_ID("userId"),
    HTTP_METHOD("httpMethod"),
    HTTP_PATH("httpPath"),
    HTTP_STATUS("httpStatus"),
    AUCTION_ID("auctionId"),
    BID_ID("bidId"),
    PAYMENT_ID("paymentId"),
    EVENT_TYPE("eventType"),
    ERROR_CODE("errorCode"),
    OPERATION("operation");

    private final String key;

    LogField(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
