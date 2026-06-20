package com.warehouse.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatUtil {
    private static final NumberFormat CURRENCY_FMT = NumberFormat.getInstance(new Locale("vi", "VN"));
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String money(BigDecimal value) {
        if (value == null) return "0 đ";
        return CURRENCY_FMT.format(value) + " đ";
    }

    public static String number(int value) {
        return CURRENCY_FMT.format(value);
    }
}
