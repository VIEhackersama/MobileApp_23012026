package com.example.fruitshop.core;

import com.example.fruitshop.models.SaleUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private Money() {}
    public static long lineTotal(long unitPrice, long baseQuantity, SaleUnit unit) {
        if (unitPrice <= 0 || baseQuantity <= 0 || unit == null) throw invalid();
        try {
            BigDecimal result = BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(baseQuantity));
            if (unit == SaleUnit.KG) result = result.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP);
            return result.longValueExact();
        } catch (ArithmeticException e) { throw invalid(); }
    }
    public static long add(long total, long next) {
        if (total < 0 || next < 0) throw invalid();
        try { return Math.addExact(total, next); }
        catch (ArithmeticException e) { throw invalid(); }
    }
    public static String format(long dong) {
        if (dong < 0) throw invalid();
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(dong) + " ₫";
    }
    private static DomainException invalid() {
        return new DomainException(ErrorCode.INVALID_INPUT, "Giá hoặc số tiền vượt phạm vi hợp lệ.");
    }
}
