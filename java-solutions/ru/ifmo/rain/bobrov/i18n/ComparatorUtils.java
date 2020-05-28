package ru.ifmo.rain.bobrov.i18n;

import java.text.Collator;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class ComparatorUtils {


    public static Comparator<ParsedData<String>> getStringLocaledComparator(Locale locale) {
        return (a, b) -> Collator.getInstance(locale).compare(a.getParsedInfo(), b.getParsedInfo());
    }


    public static Comparator<ParsedData<Number>> getNumericComparator() {
        return Comparator.comparingDouble(s -> s.getParsedInfo().doubleValue());
    }

    public static Comparator<ParsedData<Date>> getDateComparator() {
        return Comparator.comparing(ParsedData::getParsedInfo);
    }
}
