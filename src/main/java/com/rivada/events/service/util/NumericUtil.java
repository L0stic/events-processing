package com.rivada.events.service.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static java.util.Objects.isNull;

public class NumericUtil {

    public static BigDecimal getAmountFromDecimalAmount(BigDecimal decimalAmount, Integer decimalCoef) {
        if (isNull(decimalAmount) || isNull(decimalCoef)) {
            return BigDecimal.ZERO;
        }
        return decimalAmount.divide(BigDecimal.valueOf(Math.pow(10, decimalCoef)), decimalCoef, RoundingMode.HALF_UP);
    }

    public static BigDecimal getAmountFromDecimalAmount(BigInteger decimalAmount, Integer decimalCoef) {
        if (isNull(decimalAmount) || isNull(decimalCoef)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(decimalAmount)
                .divide(BigDecimal.valueOf(Math.pow(10, decimalCoef)), decimalCoef, RoundingMode.HALF_UP);
    }
}
