package org.proto68.jails.utils;

public class TimeParser {

    public static long parseTime(String input) {
        long totalSeconds = 0;

        String number = "";

        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                number += c;
            } else {
                if (number.isEmpty()) {
                    throw new IllegalArgumentException("Invalid time format");
                }

                int value = Integer.parseInt(number);

                switch (c) {
                    case 's':
                        totalSeconds += value;
                        break;
                    case 'm':
                        totalSeconds += value * 60L;
                        break;
                    case 'h':
                        totalSeconds += value * 3600L;
                        break;
                    case 'd':
                        totalSeconds += value * 86400L;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid time unit: " + c);
                }

                number = "";
            }
        }

        return totalSeconds;
    }
}