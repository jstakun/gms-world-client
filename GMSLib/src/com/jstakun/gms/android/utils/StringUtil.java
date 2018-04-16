package com.jstakun.gms.android.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import com.jstakun.gms.android.config.ConfigurationManager;


public class StringUtil {

    private static final DecimalFormat coordsFormatE6 = (DecimalFormat)NumberFormat.getInstance(java.util.Locale.US);
    private static final DecimalFormat coordsFormatE2 = (DecimalFormat)NumberFormat.getInstance(java.util.Locale.US);

    static {
        coordsFormatE2.applyPattern("##.##");
        coordsFormatE6.applyPattern("##.######");
    };

    public static String integerToString(int i) {
        String str1 = Integer.toString(i);
        if (i < 10 && i >= 0) {
            str1 = "0" + str1;
        }
        return str1;
    }

    /** Parse string to short, return defaultValue is parse fails */
    public static short parseShort(String value, short defaultValue) {
        short parsed = defaultValue;
        if (StringUtils.isNotEmpty(value)) {
            try {
                parsed = Short.parseShort(value);
            } catch (NumberFormatException e) {
                parsed = defaultValue;
            }
        }
        return parsed;
    }

    /** Parse string to int, return defaultValue is parse fails */
    public static int parseInteger(String value, int defaultValue) {
        int parsed = defaultValue;
        if (StringUtils.isNotEmpty(value)) {
            try {
                parsed = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                parsed = defaultValue;
            }
        }
        return parsed;
    }

    public static long parseLong(String longString, long defaultValue) {
        long result = defaultValue;
        if (StringUtils.isNotEmpty(longString)) {
            try {
                result = Long.parseLong(longString);
            } catch (Exception e) {
            }
        }
        return result;
    }

    /** Parse string to double, return defaultValue is parse fails */
    public static double parseDouble(String value, double defaultValue) {
        double parsed = defaultValue;
        if (StringUtils.isNotEmpty(value)) {
            try {
                parsed = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                parsed = defaultValue;
            }
        }
        return parsed;
    }

    public static String formatDouble(double value, int decimals) {

        if ((value * 1024.0) < 1.0) {
            return "0.00";
        }

        String doubleStr = "" + value;
        int index = doubleStr.indexOf(".") != -1 ? doubleStr.indexOf(".") : doubleStr.indexOf(",");
// Decimal point can not be found...
        if (index == -1) {
            return doubleStr;
        }
// Truncate all decimals
        if (decimals == 0) {
            return doubleStr.substring(0, index);
        }
        int len = index + decimals + 1;
        if (len >= doubleStr.length()) {
            len = doubleStr.length();
        }

        double d = Double.parseDouble(doubleStr.substring(0, len));
        return String.valueOf(d);
    }

    public static boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String join(List<String> s, String delimiter) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        Iterator<String> iter = s.iterator();
        StringBuilder builder = new StringBuilder(iter.next());
        while (iter.hasNext()) {
            builder.append(delimiter).append(iter.next());
        }
        return builder.toString();
    }

    public static String formatCommaSeparatedString(String source) {
        String response = source;
        String[] splittedStr = source.split(",");
        if (splittedStr.length > 1) {
            response = "";
            for (int i = 0; i < splittedStr.length; i++) {
                response += WordUtils.capitalize(StringUtils.trimToEmpty(splittedStr[i]));
                if (i < (splittedStr.length - 1)) {
                    response += ", ";
                }
            }
        }

        return response;
    }

    public static String getHashFromUrl(String url) {
        if (StringUtils.startsWith(url, ConfigurationManager.BITLY_URL)) {
            return StringUtils.remove(url, ConfigurationManager.BITLY_URL);
        } else {
            return null;
        }
    }

    public static String getKeyFromUrl(String url) {
        if (StringUtils.startsWith(url, ConfigurationManager.BITLY_URL)) {
            return url;
        } else if (StringUtils.startsWith(url, ConfigurationManager.SERVER_URL) && StringUtils.indexOf(url, "key=") > 0) {
            String[] urlSplit = url.split("key=");
            if (urlSplit.length == 2) {
                return urlSplit[1];
            }
        }
        return null;
    }

    public static List<Integer> stringToLongArray(String src, String separator) {
        List<Integer> res = new ArrayList<Integer>();
        if (src != null && separator != null) {
            try {
                String[] strArray = src.split(separator);
                if (strArray.length > 0) {
                    for (int i = 0; i < strArray.length; i++) {
                        res.add(Integer.parseInt(strArray[i]));
                    }
                }
            } catch (Exception e) {
            }
        }
        return res;
    }

    public static String getLastTokens(int count, List<Integer> tokens) {
        List<Integer> sublist = tokens;
        if (tokens.size() > count) {
            sublist = tokens.subList(tokens.size() - count, tokens.size());
        }
        return StringUtils.join(sublist, ",");
    }

    public static String formatCoordE6(double coord) {
        return coordsFormatE6.format(coord);
    }

    public static String formatCoordE2(double coord) {
        return coordsFormatE2.format(coord);
    }
    
    public static byte[] concat(byte[] b1, byte[] b2) {
        byte[] b3 = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, b3, 0, b1.length);
        System.arraycopy(b2, 0, b3, b1.length, b2.length);
        return b3;
    }
    
    public static List<String> removeDuplicates(String[] first, String[] second) {
    	List<String> result = new ArrayList<String>();
    	if (first != null) {
    		result.addAll(Arrays.asList(first));
    	}
    	if (second != null) {
    		for(int i=0;i<second.length;i++) {
    			if (!result.contains(second[i])) {
    				result.add(second[i]);
    			}
    		}
    	}
		return result;
	}
    
    public static Double decode(String val) {
    	if (val != null) {
    		StringBuilder sb = new StringBuilder();
    		for (char c : val.toCharArray()) {
    			sb.append((char)((int)c - 64));
    		}
    		return new Double(sb.toString()) / 1E6;
    	} else {
    		return null;
    	}
    }
}
