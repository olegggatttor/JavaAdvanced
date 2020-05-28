package ru.ifmo.rain.bobrov.i18n;

import java.util.ArrayList;
import java.util.Date;

public class Results {
    private ArrayList<ParsedData<Date>> dates;
    private ArrayList<ParsedData<Number>> currencies;
    private ArrayList<ParsedData<Number>> numbers;

    Results(ArrayList<ParsedData<Date>> dates, ArrayList<ParsedData<Number>> currencies, ArrayList<ParsedData<Number>> numbers) {
        this.dates = dates;
        this.currencies = currencies;
        this.numbers = numbers;
    }

    public ArrayList<ParsedData<Date>> getDates() {
        return dates;
    }

    public ArrayList<ParsedData<Number>> getCurrencies() {
        return currencies;
    }

    public ArrayList<ParsedData<Number>> getNumbers() {
        return numbers;
    }
}
