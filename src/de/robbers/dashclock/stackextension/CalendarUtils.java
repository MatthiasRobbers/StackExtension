package de.robbers.dashclock.stackextension;

public class CalendarUtils {

    public static final long ONE_DAY = 86400000; // 24 * 60 * 60 * 1000;

    public static long getToday() {
        long now = System.currentTimeMillis();
        long millisSinceGmtMidnight = System.currentTimeMillis() % (24L * 60 * 60 * 1000);
        return now - millisSinceGmtMidnight;
    }

    public static long getTomorrow() {
        long today = getToday();
        long tomorrow = today + ONE_DAY;
        return tomorrow;
    }

    public static long getOneWeekAgo() {
        long today = getToday();
        long oneWeekAgo = today - 7 * ONE_DAY;
        return oneWeekAgo;
    }
}
