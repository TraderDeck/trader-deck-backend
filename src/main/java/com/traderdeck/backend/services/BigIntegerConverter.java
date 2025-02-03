package com.traderdeck.backend.services;
import com.opencsv.bean.*;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;

public class BigIntegerConverter extends AbstractBeanField<String, String> {
    @Override
    protected Object convert(String value) {
        System.out.println("*** market cap is: "+ value);

        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value).toBigInteger();
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number format: " + value, e);
        }
    }
}
