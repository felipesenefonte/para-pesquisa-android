package com.lfdb.parapesquisa;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;


/**
 * Created by Igor on 9/6/13.
 */
public class DateParser {

    private static boolean check(StringTokenizer st, String token)
            throws InvalidDateException
    {
        try {
            if (st.nextToken().equals(token)) {
                return true;
            } else {
                throw new InvalidDateException("Missing ["+token+"]");
            }
        } catch (NoSuchElementException ex) {
            return false;
        }
    }

    private static Calendar getCalendar(String isodate)
            throws InvalidDateException
    {
        // YYYY-MM-DDThh:mm:ss.sTZD
        StringTokenizer st = new StringTokenizer(isodate, "-T:.+Z", true);

        //Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        Calendar calendar = new GregorianCalendar(TimeZone.getDefault());
        calendar.clear();
        try {
            // Year
            if (st.hasMoreTokens()) {
                int year = Integer.parseInt(st.nextToken());
                calendar.set(Calendar.YEAR, year);
            } else {
                return calendar;
            }
            // Month
            if (check(st, "-") && (st.hasMoreTokens())) {
                int month = Integer.parseInt(st.nextToken()) -1;
                calendar.set(Calendar.MONTH, month);
            } else {
                return calendar;
            }
            // Day
            if (check(st, "-") && (st.hasMoreTokens())) {
                int day = Integer.parseInt(st.nextToken());
                calendar.set(Calendar.DAY_OF_MONTH, day);
            } else {
                return calendar;
            }
            // Hour
            if (check(st, "T") && (st.hasMoreTokens())) {
                int hour = Integer.parseInt(st.nextToken());
                calendar.set(Calendar.HOUR_OF_DAY, hour);
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar;
            }
            // Minutes
            if (check(st, ":") && (st.hasMoreTokens())) {
                int minutes = Integer.parseInt(st.nextToken());
                calendar.set(Calendar.MINUTE, minutes);
            } else {
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar;
            }

            //
            // Not mandatory now
            //

            // Secondes
            if (! st.hasMoreTokens()) {
                return calendar;
            }
            String tok = st.nextToken();
            if (tok.equals(":")) { // secondes
                if (st.hasMoreTokens()) {
                    int secondes = Integer.parseInt(st.nextToken());
                    calendar.set(Calendar.SECOND, secondes);
                    if (! st.hasMoreTokens()) {
                        return calendar;
                    }
                    // frac sec
                    tok = st.nextToken();
                    if (tok.equals(".")) {
                        // bug fixed, thx to Martin Bottcher
                        String nt = st.nextToken();
                        while(nt.length() < 3) {
                            nt += "0";
                        }
                        nt = nt.substring( 0, 3 ); //Cut trailing chars..
                        int millisec = Integer.parseInt(nt);
                        //int millisec = Integer.parseInt(st.nextToken()) * 10;
                        calendar.set(Calendar.MILLISECOND, millisec);
                        if (! st.hasMoreTokens()) {
                            return calendar;
                        }
                        tok = st.nextToken();
                    } else {
                        calendar.set(Calendar.MILLISECOND, 0);
                    }
                } else {
                    throw new InvalidDateException("No secondes specified");
                }
            } else {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
            }

            // Timezone
            if (! tok.equals("Z")) { // UTC
                if (! (tok.equals("+") || tok.equals("-"))) {
                    throw new InvalidDateException("only Z, + or - allowed");
                }
                boolean plus = tok.equals("+");
                if (! st.hasMoreTokens()) {
                    throw new InvalidDateException("Missing hour field");
                }
                int tzhour = Integer.parseInt(st.nextToken());
                int tzmin  = 0;
                if (check(st, ":") && (st.hasMoreTokens())) {
                    tzmin = Integer.parseInt(st.nextToken());
                } else {
                    throw new InvalidDateException("Missing minute field");
                }
                if (plus) {
                    calendar.add(Calendar.HOUR, tzhour);
                    calendar.add(Calendar.MINUTE, tzmin);
                } else {
                    calendar.add(Calendar.HOUR, -tzhour);
                    calendar.add(Calendar.MINUTE, -tzmin);
                }
            }
        } catch (NumberFormatException ex) {
            throw new InvalidDateException("["+ex.getMessage()+
                    "] is not an integer");
        }
        return calendar;
    }

    /**
     * Parse the given string in ISO 8601 format and build a Date object.
     * @param isodate the date in ISO 8601 format
     * @return a Date instance
     * @exception InvalidDateException if the date is not valid
     */
    public static Date parse(String isodate)
            //throws InvalidDateException
            throws ParseException
    {
        /*Calendar calendar = getCalendar(isodate);
        return calendar.getTime();*/
        ISO8601DateFormat fmt = new ISO8601DateFormat();
        return fmt.parse(isodate);
    }

    private static String twoDigit(int i) {
        if (i >=0 && i < 10) {
            return "0"+String.valueOf(i);
        }
        return String.valueOf(i);
    }

    /**
     * Generate a ISO 8601 date
     * @param date a Date instance
     * @return a string representing the date in the ISO 8601 format
     */
    public static String getIsoDate(Date date) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);
        StringBuffer buffer = new StringBuffer();
        buffer.append(calendar.get(Calendar.YEAR));
        buffer.append("-");
        buffer.append(twoDigit(calendar.get(Calendar.MONTH) + 1));
        buffer.append("-");
        buffer.append(twoDigit(calendar.get(Calendar.DAY_OF_MONTH)));
        buffer.append("T");
        buffer.append(twoDigit(calendar.get(Calendar.HOUR_OF_DAY)));
        buffer.append(":");
        buffer.append(twoDigit(calendar.get(Calendar.MINUTE)));
        buffer.append(":");
        buffer.append(twoDigit(calendar.get(Calendar.SECOND)));
        buffer.append(".");
        buffer.append(twoDigit(calendar.get(Calendar.MILLISECOND) / 10));
        buffer.append("Z");
        return buffer.toString();
    }

}
