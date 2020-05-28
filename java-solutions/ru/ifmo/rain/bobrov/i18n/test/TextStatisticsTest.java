package ru.ifmo.rain.bobrov.i18n.test;

import org.junit.*;
import ru.ifmo.rain.bobrov.i18n.*;

import java.text.BreakIterator;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.function.BiFunction;


public class TextStatisticsTest {
    private TextStatistics textStatistics = new TextStatistics();

    @Before
    public void init() {
        textStatistics.initParsers(Locale.ENGLISH);
        textStatistics.setBundleLocale(Locale.ENGLISH);
    }

    //Locale.ENGLISH
    @Test
    public void numberParse() {
        String[] tests = {"1", "12345", "It is about 2000.56 meters underground.", "correctNumber: 0.2"};
        String[] answers = {"1", "12345", "2000.56", "0.2"};
        for (int i = 0; i < tests.length; i++) {
            checkParsers(answers[i], tests[i],
                    (str, pos) -> textStatistics.tryParseNumeric(str, pos, Parsers.NUMBER));
        }
    }

    //Locale.ENGLISH
    @Test
    public void dateParse() {
        String[] tests = {"05/27/2000",
                "This is MEDIUM mode: May 27, 2020",
                "Full mode looks like this: Wednesday, May 27, 2020. And you should parse only first and not this: May 27, 2020",
                "This is not a valid date: 12.25.18"};
        String[] answers = {"05/27/2000", "May 27, 2020", "Wednesday, May 27, 2020", ""};
        for (int i = 0; i < tests.length; i++) {
            checkParsers(answers[i], tests[i],
                    (str, pos) -> textStatistics.tryParseDates(str, pos));
        }
    }

    //Locale.de_DE
    @Test
    public void currencyParse() {
        textStatistics.initParsers(new Locale("de", "DE"));
        String[] tests = {"10.789,80 €",
                "This is number 10.789,80 and this is currency 10.789,80 €",
                "This is not euro 10 $ but parse this 90 €",
                "90€ nothing $10 to 90.50.20 € parse"};
        String[] answers = {"10.789,80 €", "10.789,80 €", "90 €", ""};
        for (int i = 0; i < tests.length; i++) {
            checkParsers(answers[i], tests[i],
                    (str, pos) -> textStatistics.tryParseNumeric(str, pos, Parsers.CURRENCY));
        }
    }

    //Locale.ENGLISH
    @Test
    public void wordsParse() {
        String[] tests = {"Just three words.",
                "It is 10 o'clock. Second sentence - it is okay.",
                "",
                "It is number 10.22 , not second sentence."};
        String[][] answers = {{"Just", "three", "words"},
                {"It", "is", "10", "o'clock", "Second", "sentence", "it", "is", "okay"},
                {},
                {"It", "is", "number", "10.22", "not", "second", "sentence"}};
        Locale testLocale = Locale.ENGLISH;
        checkVocabulary(tests, answers, BreakIterator.getWordInstance(testLocale), testLocale);
    }

    //Locale.ENGLISH
    @Test
    public void sentencesParse() {
        String[] tests = {"Just three words.",
                "It is 10 o'clock. Second sentence - it is okay.",
                "",
                "It is number 10.22 , not second sentence.",
                "A. B. C. D. E."};
        String[][] answers = {{"Just three words."},
                {"It is 10 o'clock.", "Second sentence - it is okay."},
                {},
                {"It is number 10.22 , not second sentence."},
                {"A.", "B.", "C.", "D.", "E."}};
        Locale testLocale = Locale.ENGLISH;
        checkVocabulary(tests, answers, BreakIterator.getSentenceInstance(testLocale), testLocale);
    }

    //Locale.Arabic_UAE
    @Test
    public void testAR_AE() {
        Locale testLocale = new Locale("ar", "AE");
        String test = "مرحبا! كيف حالك! قل ٢ نقاط!";
        String[] sentenceStats = {" 3 (3 unique)", " قل ٢ نقاط!", " مرحبا!",
                String.format(" %d (%s)", "مرحبا!".length(), "مرحبا!"),
                String.format(" %d (%s)", "قل ٢ نقاط!".length(), "قل ٢ نقاط!"),
                String.format(" %s", "8,333")};
        String[] wordStat = {" 6 (6 unique)", " ٢", " نقاط",
                String.format(" %d (%s)", "٢".length(), "٢"),
                String.format(" %d (%s)", "مرحبا".length(), "مرحبا"),
                String.format(" %s", "3,167")};
        String[] numbStat = {" 1 (1 unique)", " ٢", " ٢",
                String.format(" %d (%s)", "٢".length(), "٢"),
                String.format(" %d (%s)", "٢".length(), "٢"),
                String.format(" %s", "1,000")};
        String[] currencyStat = {"NONE"};
        String[] dateStat = {"NONE"};
        String[][] answers = {sentenceStats, wordStat, numbStat, currencyStat, dateStat};
        parseAndCheckBlocksLocaled(test, answers, testLocale);
    }


    //Locale.en_US
    @Test
    public void testEN_US() {
        Locale testLocale = new Locale("en", "US");
        String test = "Hello! My name is John. I live in Moscow and I am 30 years old. My salary is $10,789.80 per month. " +
                "I was bor in May 28, 1990. My wife was born in 5/28/85 and now she is 35 years old.";
        String[] sentenceStats = {" 6 (6 unique)", " Hello!", " My wife was born in 5/28/85 and now she is 35 years old.",
                String.format(" %d (%s)", "Hello!".length(), "Hello!"),
                String.format(" %d (%s)", "My wife was born in 5/28/85 and now she is 35 years old.".length(),
                        "My wife was born in 5/28/85 and now she is 35 years old."),
                String.format(" %s", "29,500")};
        String[] wordStat = {" 43 (30 unique)", " $10,789.80", " years",
                String.format(" %d (%s)", "I".length(), "I"),
                String.format(" %d (%s)", "$10,789.80".length(), "$10,789.80"),
                String.format(" %s", "3,093")};
        String[] numStat = {" 2 (2 unique)", " 30", " 35",
                String.format(" %d (%s)", "30".length(), "30"),
                String.format(" %d (%s)", "35".length(), "35"),
                String.format(" %s", "2,000")};
        String[] currencyStat = {" 1 (1 unique)", " $10,789.80", " $10,789.80",
                String.format(" %d (%s)", "$10,789.80".length(), "$10,789.80"),
                String.format(" %d (%s)", "$10,789.80".length(), "$10,789.80"),
                String.format(" %s", "10,000")};
        String[] dateStat = {" 2 (2 unique)", " 5/28/85", " May 28, 1990",
                String.format(" %d (%s)", "5/28/85".length(), "5/28/85"),
                String.format(" %d (%s)", "May 28, 1990".length(), "May 28, 1990"),
                String.format(" %s", "9,500")};
        String[][] answers = {sentenceStats, wordStat, numStat, currencyStat, dateStat};
        parseAndCheckBlocksLocaled(test, answers, testLocale);
    }

    //Locale.ru_RU
    @Test
    public void testRU_RU() {
        Locale testLocale = new Locale("ru", "RU");
        String test = "Сегодня хороший день - 28 мая 2020 г. . " +
                "Светит солнце, а улице 17 градусов." +
                " Крантин продолжается уже 2-ой месяц и закончится примерно в воскресение, 7 июня 2020 г. ." +
                " В кармане 12 ₽, грустно. А $12 - это число, а не Американские доллары.";
        String[] sentenceStats = {" 5 (5 unique)", " А $12 - это число, а не Американские доллары.", " Сегодня хороший день - 28 мая 2020 г. .",
                String.format(" %d (%s)", "В кармане 12 ₽, грустно.".length(), "В кармане 12 ₽, грустно."),
                String.format(" %d (%s)", "Крантин продолжается уже 2-ой месяц и закончится примерно в воскресение, 7 июня 2020 г. .".length(),
                        "Крантин продолжается уже 2-ой месяц и закончится примерно в воскресение, 7 июня 2020 г. ."),
                String.format(" %s", "46,400")};
        String[] wordStat = {" 40 (35 unique)", " $12", " это",
                String.format(" %d (%s)", "г".length(), "г"),
                String.format(" %d (%s)", "Американские".length(), "Американские"),
                String.format(" %s", "4,450")};
        String[] numStat = {" 3 (3 unique)", " 2", " 17",
                String.format(" %d (%s)", "2".length(), "2"),
                String.format(" %d (%s)", "12".length(), "12"),
                String.format(" %s", "1,667")};
        String[] currencyStat = {" 1 (1 unique)", " 12 ₽", " 12 ₽",
                String.format(" %d (%s)", "12 ₽".length(), "12 ₽"),
                String.format(" %d (%s)", "12 ₽".length(), "12 ₽"),
                String.format(" %s", "4,000")};
        String[] dateStat = {" 2 (2 unique)", " 28 мая 2020 г.", " 7 июня 2020 г.",
                String.format(" %d (%s)", "28 мая 2020 г.".length(), "28 мая 2020 г."),
                String.format(" %d (%s)", "7 июня 2020 г.".length(), "7 июня 2020 г."),
                String.format(" %s", "14,000")};
        String[][] answers = {sentenceStats, wordStat, numStat, currencyStat, dateStat};
        parseAndCheckBlocksLocaled(test, answers, testLocale);
    }

    //Locale.es_ARGENTINA
    @Test
    public void testES_AR() {
        Locale testLocale = new Locale("es", "AR");
        String test = "¡Bienvenido a ITMO! Esta universidad fue fundada el 25 de marzo de 1900. En promedio, la matrícula cuesta $ 2.000. Entré allí a los 18 años y estoy estudiando por segundo año.";
        String[] sentenceStats = {" 4 (4 unique)", " ¡Bienvenido a ITMO!", " Esta universidad fue fundada el 25 de marzo de 1900.",
                String.format(" %d (%s)", "¡Bienvenido a ITMO!".length(), "¡Bienvenido a ITMO!"),
                String.format(" %d (%s)", "Entré allí a los 18 años y estoy estudiando por segundo año.".length(),
                        "Entré allí a los 18 años y estoy estudiando por segundo año."),
                String.format(" %s", "43,000")};
        String[] wordStat = {" 31 (29 unique)", " 18", " y",
                String.format(" %d (%s)", "a".length(), "a"),
                String.format(" %d (%s)", "universidad".length(), "universidad"),
                String.format(" %s", "4,419")};
        String[] numStat = {" 1 (1 unique)", " 18", " 18",
                String.format(" %d (%s)", "18".length(), "18"),
                String.format(" %d (%s)", "18".length(), "18"),
                String.format(" %s", "2,000")};
        String[] currencyStat = {" 1 (1 unique)", " $ 2.000", " $ 2.000",
                String.format(" %d (%s)", "$ 2.000".length(), "$ 2.000"),
                String.format(" %d (%s)", "$ 2.000".length(), "$ 2.000"),
                String.format(" %s", "7,000")};
        String[] dateStat = {" 1 (1 unique)", " 25 de marzo de 1900", " 25 de marzo de 1900",
                String.format(" %d (%s)", "25 de marzo de 1900".length(), "25 de marzo de 1900"),
                String.format(" %d (%s)", "25 de marzo de 1900".length(), "25 de marzo de 1900"),
                String.format(" %s", "19,000")};
        String[][] answers = {sentenceStats, wordStat, numStat, currencyStat, dateStat};
        parseAndCheckBlocksLocaled(test, answers, testLocale);
    }


    private void parseAndCheckBlocksLocaled(String test, String[][] answers, Locale testLocale) {
        textStatistics.initParsers(testLocale);
        Results numeric = textStatistics.extractAll(test);
        Comparator<ParsedData<String>> stringComparator = ComparatorUtils.getStringLocaledComparator(testLocale);
        Comparator<ParsedData<Number>> numericComparator = ComparatorUtils.getNumericComparator();
        ArrayList<ParsedData<Date>> dates = numeric.getDates();
        ArrayList<ParsedData<Number>> currencies = numeric.getCurrencies();
        ArrayList<ParsedData<Number>> numbers = numeric.getNumbers();
        checkStatisticBlock(textStatistics
                        .splitByIterator(BreakIterator.getSentenceInstance(testLocale), test, testLocale),
                stringComparator, answers[0], Collator.getInstance(testLocale));
        checkStatisticBlock(textStatistics
                        .splitByIterator(BreakIterator.getWordInstance(testLocale), test, testLocale),
                stringComparator, answers[1], Collator.getInstance(testLocale));
        checkStatisticBlock(numbers, numericComparator, answers[2], Collator.getInstance(testLocale));
        checkStatisticBlock(currencies, numericComparator, answers[3], Collator.getInstance(testLocale));
        checkStatisticBlock(dates, ComparatorUtils.getDateComparator(), answers[4], Collator.getInstance(testLocale));
    }


    private <T> void checkStatisticBlock(ArrayList<ParsedData<T>> blockData, Comparator<ParsedData<T>> comparator,
                                         String[] answers, Collator lexicComp) {
        String[] results = textStatistics.process(blockData, comparator);
        for (int i = 0; i < results.length; i++) {
            if (lexicComp.compare(results[i], answers[i]) != 0) {
                Assert.fail("TEST: " + (i + 1) + "\n" + results[i] + " +++ " + answers[i]);
            }
        }
    }

    private void checkVocabulary(String[] tests, String[][] answers, BreakIterator iterator, Locale locale) {
        for (int i = 0; i < tests.length; i++) {
            String[] parsedData = textStatistics.splitByIterator(iterator, tests[i], locale)
                    .stream()
                    .map(ParsedData::getParsedString)
                    .toArray(String[]::new);
            Assert.assertArrayEquals(answers[i], parsedData);
        }
    }

    private <T> void checkParsers(String expected, String str, BiFunction<String, Integer, ParsedData<T>> f) {
        for (int pos = 0; pos < str.length(); pos++) {
            ParsedData<T> parsed = f.apply(str, pos);
            if (parsed.getParsedType() != Parsers.DEFAULT) {
                parsed.attachString(str.substring(pos, parsed.getPos()));
                Assert.assertEquals(expected, parsed.getParsedString());
                return;
            }
        }
        Assert.assertEquals(expected, "");
    }

}
