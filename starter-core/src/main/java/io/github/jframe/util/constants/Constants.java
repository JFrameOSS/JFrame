package io.github.jframe.util.constants;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Container class for application constants organized by category.
 */
public final class Constants {

    private Constants() {
        // Private constructor to prevent instantiation
    }

    /**
     * Constants used in percentages.
     */
    @UtilityClass
    public static class Percentages {

        public static final double ZERO_PERCENT = 0.00;
        public static final double FIFTY_PERCENT = 50.00;
        public static final double ONE_HUNDRED_PERCENT = 100.00;
    }


    /**
     * Constants for HTTP headers.
     */
    @UtilityClass
    public static class Headers {

        public static final String TX_ID_HEADER = "x-transaction-id";
        public static final String REQ_ID_HEADER = "x-request-id";
        public static final String TRACE_ID_HEADER = "x-trace-id";
        public static final String SPAN_ID_HEADER = "x-span-id";
        public static final String L7_REQUEST_ID = "X-Layer7-Requestid";
        public static final String X_CLIENT_VERSION = "X-Client-Version";

    }


    /**
     * Constants for protocols.
     */
    @UtilityClass
    public static class Protocols {

        public static final String HTTP = "http";
        public static final String HTTPS = "https";
        public static final String TLS = "TLS";

    }


    /**
     * Character constants which are used in various contexts.
     */
    @UtilityClass
    public static class Characters {

        public static final Character EQUALS = '=';

        public static final Character AMPERSAND = '&';

        public static final Character QUOTE = '"';

        public static final Character LT = '<';

        public static final Character NEW_LINE = '\n';

        public static final String SYSTEM_NEW_LINE = System.lineSeparator();

        public static final Character CARRIAGE_RETURN = '\r';

        public static final Character JSON_ESCAPE = '\\';

        public static final Character COLON = ':';
    }


    /**
     * Class containing character constants for CSV files.
     */
    @UtilityClass
    public static class CSVConstants {

        public static final String CSV_EXTENSION = ".csv";
        public static final String DEFAULT_LINE_END = "\n";
        public static final char NO_QUOTE_CHARACTER = '\u0000';
        public static final char DEFAULT_SEPARATOR = ',';
        public static final char DEFAULT_ESCAPE_CHARACTER = '"';
        public static final char SEMICOLON_SEPARATOR = ';';
    }


    /**
     * Constants used in time.
     */
    @UtilityClass
    public static class Time {

        public static final String DAYS = "DAYS";
        public static final String HOURS = "HOURS";
        public static final String MINUTES = "MINUTES";
        public static final String SECONDS = "SECONDS";
        public static final String MILLIS = "MILLIS";

        public static final int DAYS_PER_MONTH = 30;
        public static final int HOURS_PER_DAY = 24;
        public static final int DAYS_PER_WEEK = 7;
        public static final int HOURS_A_DAY = 24;
        public static final int MINUTES_PER_HOUR = 60;
        public static final int SECONDS_PER_MINUTE = 60;
        public static final int MILLIS_PER_SECOND = 1_000;
        public static final long NANOS_PER_SECOND = 1_000_000_000;

        public static final int MILLION = 1_000_000;
        public static final int THOUSAND = 1_000;
        public static final int HUNDRED = 100;
    }


    /**
     * Constants used in RSA key properties.
     */
    @UtilityClass
    public static class KeyGeneration {

        public static final String RSA = "RSA";
        public static final int KEY_SIZE = 2048;
    }


    /**
     * Constants used in password validation.
     */
    @UtilityClass
    public static class Encoder {

        public static final String BCRYPT = "bcrypt";
        public static final String SCRYPT = "scrypt";
        public static final String PBKDF2 = "pbkdf2";
    }


    /**
     * Constants used in intervals.
     */
    @UtilityClass
    public static class Intervals {

        public static final int INTERVALS_PER_HOUR = 2;
        public static final int MINUTES_PER_INTERVAL = Time.MINUTES_PER_HOUR / INTERVALS_PER_HOUR;
        public static final long NANOS_PER_INTERVAL = MINUTES_PER_INTERVAL * Time.SECONDS_PER_MINUTE * Time.NANOS_PER_SECOND;
    }


    /**
     * Constants used in date time.
     */
    @UtilityClass
    public static class DateTime {

        public static final String DEFAULT_TIMEZONE = "UTC";
        public static final ZoneId EUROPE_AMSTERDAM = ZoneId.of("Europe/Amsterdam");

        public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        public static final LocalDateTime START_OF_EPOCH = LocalDateTime.parse("1970-01-01T00:00:00");
    }
}
