package org.jabref.model.entry;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Date {

    private final TemporalAccessor date;

    public Date(TemporalAccessor date) {
        this.date = date;
    }

    public String getNormalized() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("uuuu[-MM][-dd]");
        return dateFormatter.format(date);
    }

    public Optional<Integer> getYear() {
        return get(ChronoField.YEAR);
    }

    public Optional<Integer> get(ChronoField field) {
        if (date.isSupported(field)) {
            return Optional.of(date.get(field));
        } else {
            return Optional.empty();
        }
    }

    public Optional<Integer> getMonth() {
        return get(ChronoField.MONTH_OF_YEAR);
    }

    public Optional<Integer> getDay() {
        return get(ChronoField.DAY_OF_MONTH);
    }

    /**
     * Try to parse the following formats
     *  - "M/y" (covers 9/15, 9/2015, and 09/2015)
     *  - "MMMM (dd), yyyy" (covers September 1, 2015 and September, 2015)
     *  - "yyyy-MM-dd" (covers 2009-1-15)
     *  - "d.M.uuuu" (covers 15.1.2015)
     *  - "uuuu.M.d" (covers 2015.1.15)
     * The code is essentially taken from http://stackoverflow.com/questions/4024544/how-to-parse-dates-in-multiple-formats-using-simpledateformat.
     */
    public static Optional<Date> parse(String dateString) {
        List<String> formatStrings = Arrays.asList("uuuu-M-d", "uuuu-M", "M/uu", "M/uuuu", "MMMM d, uuuu", "MMMM, uuuu",
                "d.M.uuuu", "uuuu.M.d", "uuuu");
        for (String formatString : formatStrings) {
            try {
                TemporalAccessor parsedDate = DateTimeFormatter.ofPattern(formatString).parse(dateString);
                return Optional.of(new Date(parsedDate));
            } catch (DateTimeParseException ignored) {
                // Ignored
            }
        }

        return Optional.empty();
    }

    public static Optional<Date> parse(Optional<String> yearValue, Optional<String> monthValue, Optional<String> dayValue) {
        Optional<Year> year = yearValue.flatMap(Date::convertToInt).map(Year::of);
        Optional<Month> month = monthValue.flatMap(Month::parse);
        Optional<Integer> day = dayValue.flatMap(Date::convertToInt);


        if (year.isPresent()) {
            TemporalAccessor date;
            if (month.isPresent()) {
                if (day.isPresent()) {
                    date = LocalDate.of(year.get().getValue(), month.get().getNumber(), day.get());
                } else {
                    date = YearMonth.of(year.get().getValue(), month.get().getNumber());
                }
            } else {
                date = year.get();
            }

            return Optional.of(new Date(date));
        }

        return Optional.empty();
    }

    private static Optional<Integer> convertToInt(String value) {
        try {
            return Optional.of(Integer.valueOf(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
