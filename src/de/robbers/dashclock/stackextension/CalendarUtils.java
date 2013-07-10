package de.robbers.dashclock.stackextension;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class CalendarUtils {
    public static Calendar getToday() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return convertToGmt(today);
    }

    public static Calendar getTomorrow() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        return convertToGmt(tomorrow);
    }

    public static Calendar convertToGmt(Calendar calendar) {
        Date date = calendar.getTime();
        TimeZone timeZone = calendar.getTimeZone();

        // number of milliseconds since January 1, 1970, 00:00:00 GMT
        long msFromEpochGmt = date.getTime();

        // get you the current offset in ms from GMT at the current date
        int offsetFromUTC = timeZone.getOffset(msFromEpochGmt);

        // create a new calendar in GMT timezone, set to this date and add the offset
        Calendar gmtCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        gmtCalendar.setTime(date);
        gmtCalendar.add(Calendar.MILLISECOND, offsetFromUTC);
        return gmtCalendar;
    }
}
