package com.example.fruitshop.core;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class BusinessDates {
    private BusinessDates() {}
    private static SimpleDateFormat formatter(BusinessClock clock) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setTimeZone(clock.timeZone()); format.setLenient(false);
        return format;
    }
    public static String today(BusinessClock clock) { return formatter(clock).format(new Date(clock.nowMillis())); }
    public static String plusDays(BusinessClock clock, int days) {
        Calendar calendar = Calendar.getInstance(clock.timeZone(), Locale.ROOT);
        calendar.setTimeInMillis(clock.nowMillis()); calendar.add(Calendar.DAY_OF_MONTH, days);
        return formatter(clock).format(calendar.getTime());
    }
    public static boolean isValid(String isoDate) {
        if (isoDate == null || !isoDate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return false;
        ParsePosition position = new ParsePosition(0);
        Date parsed = formatter(BusinessClock.fixed(0, "UTC")).parse(isoDate, position);
        return parsed != null && position.getIndex() == isoDate.length();
    }
    public static boolean isExpired(String isoDate, BusinessClock clock) {
        if (!isValid(isoDate)) throw new DomainException(ErrorCode.INVALID_INPUT, "Ngày hết hạn không hợp lệ.");
        return isoDate.compareTo(today(clock)) < 0;
    }
}
