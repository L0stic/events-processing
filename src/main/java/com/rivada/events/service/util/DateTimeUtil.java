package com.rivada.events.service.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.util.Objects.isNull;

public class DateTimeUtil {
    //rounded date time minus seconds
    private static final long ROUND_TIMESTAMP_COEF_MILLISEC = 10 * 1000;

    /**
     * get timestamp from DateTime with milliseconds
     * @param dateTime the date for convert
     * @return timsetamp
     */
    public static Long getTimestamp(LocalDateTime dateTime) {
        if (isNull(dateTime)) {
            return null;
        }
        return dateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public static LocalDateTime getDateTime(Long timestamp) {
        if (isNull(timestamp) || timestamp <= 0L) {
            return null;
        }
        int length = (int) (Math.log10(timestamp) + 1);
        if (length > 10) {
            // normalize timestamp to milliseconds format
            int standardizeCoef = (int) Math.pow(10, 13 - length);
            return getDateTimeWithMilli(timestamp * standardizeCoef);
        }
        return getDateTimeWithSec(timestamp);
    }

    public static Long getTimestampInSeconds(Long timestamp) {
        if (isNull(timestamp) || timestamp <= 0L) {
            return null;
        }
        int length = (int) (Math.log10(timestamp) + 1);
        if (length > 10) {
            return timestamp / 1000;
        }
        return timestamp;
    }

    private static LocalDateTime getDateTimeWithSec(Long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC);
    }

    private static LocalDateTime getDateTimeWithMilli(Long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
    }
    @SuppressWarnings("ConstantConditions")
    public static Long getRoundedTimestamp(LocalDateTime dateTime) {
        if (isNull(dateTime)) {
            return null;
        }
        Long timestamp = getTimestamp(dateTime);
        //round timestamp upon 10 secs
        return timestamp - timestamp % ROUND_TIMESTAMP_COEF_MILLISEC;
    }

}
