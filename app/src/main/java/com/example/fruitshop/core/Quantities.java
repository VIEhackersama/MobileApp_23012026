package com.example.fruitshop.core;

import com.example.fruitshop.models.SaleUnit;
import java.math.BigDecimal;

public final class Quantities {
    private Quantities() {}
    /** Accepts zero for stock input; callers placing orders must additionally require > 0. */
    public static long parse(String input, SaleUnit unit) {
        if (input == null || unit == null) throw invalid();
        String value = input.trim();
        String pattern = unit == SaleUnit.KG ? "[0-9]+([.,][0-9]{1,3})?" : "[0-9]+";
        if (!value.matches(pattern)) throw invalid();
        try {
            BigDecimal number = new BigDecimal(value.replace(',', '.'));
            return (unit == SaleUnit.KG ? number.movePointRight(3) : number).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) { throw invalid(); }
    }
    public static String format(long baseQuantity, SaleUnit unit) {
        if (baseQuantity < 0 || unit == null) throw invalid();
        String value = unit == SaleUnit.KG
                ? BigDecimal.valueOf(baseQuantity, 3).stripTrailingZeros().toPlainString().replace('.', ',')
                : Long.toString(baseQuantity);
        return value + " " + unit.label;
    }
    private static DomainException invalid() {
        return new DomainException(ErrorCode.INVALID_INPUT, "Số lượng không hợp lệ: kg tối đa 3 số lẻ; quả/phần phải là số nguyên.");
    }
}
