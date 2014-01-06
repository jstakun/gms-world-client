package com.jstakun.gms.android.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.jstakun.gms.android.config.ConfigurationManager;

public abstract class DateTimeUtils {

    public static final long ONE_MINUTE = 60 * 1000;
    public static final long THIRTY_SECONDS = 30 * 1000;
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    public static final long FIVE_MINUTES = 5 * ONE_MINUTE;
    public static final long ONE_DAY = 24 * ONE_HOUR;
    public static final long ONE_MONTH = 31 * ONE_DAY;
    
    private static final Map<String, DateFormat> dateFormats = new HashMap<String, DateFormat>();
    private static final DateFormat yearMonthFormat = new SimpleDateFormat("MMMM yyyy", ConfigurationManager.getInstance().getCurrentLocale());
    
    /** Get current Date stamp
     *  @return The Current Date/Time in the format: yyyymmdd_hhmm 
     */
    public static String getCurrentDateStamp() {
        return convertToDateStamp(System.currentTimeMillis());
    }

    /** Convert given date to string<br>
     *  OutputFormat: yyyymmdd_hhmm
     *  @return The Date/Time in the format: yyyymmdd_hhmm
     */
    public static String convertToDateStamp(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(time));
        return convertToDateStamp(cal);
    }

    /** Convert given date to string<br>
     *  OutputFormat: yyyymmdd_hhmm
     *  @return The Date/Time in the format: yyyymmdd_hhmm
     */
    public static String convertToDateStamp(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return convertToDateStamp(cal);
    }

    /** Convert given date to string<br>
     *  OutputFormat: yyyymmdd_hhmm
     *  @return The Date/Time in the format: yyyymmdd_hhmm
     */
    public static String convertToDateStamp(Calendar cal) {
        String year = String.valueOf(cal.get(Calendar.YEAR));
        String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
        if (month.length() == 1) {
            month = "0" + month;
        }
        String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        if (day.length() == 1) {
            day = "0" + day;
        }
        String hour = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
        if (hour.length() == 1) {
            hour = "0" + hour;
        }
        String minute = String.valueOf(cal.get(Calendar.MINUTE));
        if (minute.length() == 1) {
            minute = "0" + minute;
        }
        String second = String.valueOf(cal.get(Calendar.SECOND));
        if (second.length() == 1) {
            second = "0" + second;
        }
        String dateStamp = year + month + day + "_" + hour + minute + second;
        return dateStamp;
    }

    /** 
     * Get current time stamp in universal format<br>
     * Format: yyyy-mm-ddThh:mm:ssZ<br>
     * e.g.: 1999-09-09T13:10:40Z
     * @return The Date in the format: yyyy-mm-ddThh:mm:ssZ
     */
    public static String getUniversalDateStamp(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(time));
        return getUniversalDateStamp(cal);
    }

    /** 
     * Get current time stamp in universal format<br>
     * Format: yyyy-mm-ddThh:mm:ssZ<br>
     * e.g.: 1999-09-09T13:10:40Z
     * @return The Date in the format: yyyy-mm-ddThh:mm:ssZ
     */
    public static String getUniversalDateStamp(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return getUniversalDateStamp(cal);
    }

    /** 
     * Get current time stamp in universal format<br>
     * Format: yyyy-mm-ddThh:mm:ssZ<br>
     * e.g.: 1999-09-09T13:10:40Z
     * @return The Date in the format: yyyy-mm-ddThh:mm:ssZ  
     */
    public static String getUniversalDateStamp(Calendar cal) {
        String year = String.valueOf(cal.get(Calendar.YEAR));
        String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
        if (month.length() == 1) {
            month = "0" + month;
        }
        String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        if (day.length() == 1) {
            day = "0" + day;
        }
        String hour = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
        if (hour.length() == 1) {
            hour = "0" + hour;
        }
        String minute = String.valueOf(cal.get(Calendar.MINUTE));
        if (minute.length() == 1) {
            minute = "0" + minute;
        }
        String second = String.valueOf(cal.get(Calendar.SECOND));
        if (second.length() == 1) {
            second = "0" + second;
        }
        String dateStamp = year + "-" + month + "-" + day + "T" + hour + ":"
                + minute + ":" + second + "Z";
        return dateStamp;
    }

    /** Convert date to short time string 
     * @return The Date in the format: hh:mm:ss  
     */
    public static String convertToTimeStamp(long time) {
        return convertToTimeStamp(time, true);
    }

    /** Convert date to short time string 
     * @return The Date in the format: hh:mm:ss  
     */
    public static String convertToTimeStamp(Date date) {
        return convertToTimeStamp(date, true);
    }

    /** Convert date to short time string
     * @return The Date in the format: hh:mm:ss  
     */
    public static String convertToTimeStamp(Calendar cal) {
        return convertToTimeStamp(cal, true);
    }

    /** Convert date to short time string 
     * @param showSeconds Wheather or not to show just the hours and minutes part, or to show the seconds part also.
     * @return The Date in the format: hh:mm:ss  
     */
    public static String convertToTimeStamp(long time, boolean showSeconds) {
        return convertToTimeStamp(new Date(time), showSeconds);
    }

    /** Convert date to short time string 
     * @param showSeconds Wheather or not to show just the hours and minutes part, or to show the seconds part also.
     * @return The Date in the format: hh:mm:ss  
     */
    public static String convertToTimeStamp(Date date, boolean showSeconds) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return convertToTimeStamp(c, showSeconds);
    }

    /**
     * @param time 
     * @param showSeconds Wheather or not to show just the hours and minutes part, or to show the seconds part also.
     * @return The Date in the format: hh:mm:ss
     */
    private static String convertToTimeStamp(Calendar time, boolean showSeconds) {
        String hours = Integer.toString(time.get(Calendar.HOUR_OF_DAY));
        if (hours.length() == 1) {
            hours = '0' + hours;
        }
        String minutes = Integer.toString(time.get(Calendar.MINUTE));
        if (minutes.length() == 1) {
            minutes = '0' + minutes;
        }
        if (showSeconds) {
            String seconds = Integer.toString(time.get(Calendar.SECOND));
            if (seconds.length() == 1) {
                seconds = '0' + seconds;
            }
            return hours + ":" + minutes + ":" + seconds;
        } else {
            return hours + ":" + minutes;
        }
    }

    /** 
     * 
     * @param startDate Interval start date time
     * @param endDate Interval end date time
     * @return Time interval in format hh:mm:ss
     */
    
    public static String getTimeInterval(long interval) {
        if (interval <= 0) {
            return "00:00:00";
        }
        long intervalSeconds = interval / 1000;
        long hours = intervalSeconds / 3600;
        long minutes = (intervalSeconds % 3600) / 60;
        long seconds = intervalSeconds % 60;
        String hoursText = String.valueOf(hours);
        if (hoursText.length() == 1) {
            hoursText = "0" + hoursText;
        }
        String minutesText = String.valueOf(minutes);
        if (minutesText.length() == 1) {
            minutesText = "0" + minutesText;
        }
        String secondsText = String.valueOf(seconds);
        if (secondsText.length() == 1) {
            secondsText = "0" + secondsText;
        }
        return hoursText + ":" + minutesText + ":" + secondsText;
    }

    public static String convertSecondsToTimeStamp(int seconds) {
        String hours = Integer.toString(seconds / 3600);
        if (hours.length() == 1) {
            hours = '0' + hours;
        }
        String minutes = Integer.toString((seconds % 3600) / 60);
        if (minutes.length() == 1) {
            minutes = '0' + minutes;
        }

        String secondsStr = Integer.toString(seconds % 60);
        if (secondsStr.length() == 1) {
            secondsStr = '0' + secondsStr;
        }
        return hours + ":" + minutes + ":" + secondsStr;
    }

    public static String getDefaultDateTimeString(long date, Locale locale) {
        return getDateTimeString(new Date(date), DateFormat.DEFAULT, locale);
    }

    public static String getDefaultDateTimeString(String date, Locale locale) {
        try {
            long millis = Long.parseLong(date);
            return getDateTimeString(new Date(millis), DateFormat.DEFAULT, locale);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getShortDateTimeString(long date, Locale locale) {
        return getDateTimeString(new Date(date), DateFormat.SHORT, locale);
    }

    public static String getShortDateTimeString(String date, Locale locale) {
        try {
            long millis = Long.parseLong(date);
            return getDateTimeString(new Date(millis), DateFormat.SHORT, locale);
        } catch (Exception e) {
            return "";
        }
    }

    private static String getDateTimeString(Date date, int format, Locale currentLocale) {
        
        String key = format + "_" + currentLocale.toString();
        
        DateFormat formatter = dateFormats.get(key);
        
        if (formatter == null) {
            formatter = DateFormat.getDateTimeInstance(format, format, currentLocale);
            dateFormats.put(key, formatter);
        }
        
        return formatter.format(date);
    }

    public static boolean isAtMostNMonthsAgo(long date, int howMany) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -howMany);
        Date d = new Date(date);
        return d.after(cal.getTime());
    }

    public static boolean isAtMostNWeeksAgo(long date, int howMany) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, -howMany);
        Date d = new Date(date);
        return d.after(cal.getTime());
    }
    
    public static String getYearMonth(int year, int month) {
    	Calendar cal = Calendar.getInstance();
    	cal.set(Calendar.YEAR, year);
    	cal.set(Calendar.MONTH, month);
    	return yearMonthFormat.format(cal.getTime());
    }
    
    public static String getDateString(long date, int style, Locale locale) {
    	DateFormat formatter = DateFormat.getDateInstance(style, locale);
    	return formatter.format(date);
    }
    
    public static String getShortDateString(long date, Locale locale) {
    	return getDateString(date, DateFormat.SHORT, locale);
    }
}
