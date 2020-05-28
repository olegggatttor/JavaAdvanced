package ru.ifmo.rain.bobrov.i18n;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.stream.Collectors;

public class TextStatistics {
    private static Format[] parsers = new Format[6];
    private static int[] styles = new int[]{DateFormat.FULL, DateFormat.LONG, DateFormat.MEDIUM, DateFormat.SHORT};
    private static ResourceBundle bundle = ResourceBundle.getBundle("ru.ifmo.rain.bobrov.i18n.LocaleResourceBundle", Locale.ENGLISH);
    private static final String[] bundleKeys = {"amount", "minValue", "maxValue", "minLength", "maxLength", "avgLength", "stat"};
    private static final String[] categories = {"sentence", "lines", "words", "numbers", "money", "dates"};


    public TextStatistics() {
        initParsers(Locale.getDefault());
    }


    public void setBundleLocale(Locale locale) {
        bundle = ResourceBundle.getBundle("ru.ifmo.rain.bobrov.i18n.LocaleResourceBundle", locale);
    }

    public void initParsers(Locale locale) {
        for (int i = 0; i < 4; i++) {
            parsers[i] = DateFormat.getDateInstance(styles[i], locale);
        }
        parsers[4] = NumberFormat.getCurrencyInstance(locale);
        parsers[5] = NumberFormat.getNumberInstance(locale);
    }


    public static void main(String[] args) {
        if (!validateArguments(args)) {
            System.err.println(bundle.getString("wa"));
            return;
        }
        Path pathIn;
        Path pathOut;
        try {
            pathIn = Paths.get(args[2]);
            pathOut = Paths.get(args[3]);
        } catch (InvalidPathException e) {
            System.out.println("Input or output path is incorrect.");
            return;
        }
        if (pathOut.getParent() != null) {
            if (!Files.exists(pathOut.getParent())) {
                try {
                    Files.createDirectories(pathOut.getParent());
                } catch (IOException e) {
                    System.out.println("Impossible to create parent directory.");
                    return;
                }
            }
        }
        String[] localeTrimmed = args[0].split("_");
        Locale inputLocale;
        switch (localeTrimmed.length) {
            case 1:
                inputLocale = new Locale(localeTrimmed[0]);
                break;
            case 2:
                inputLocale = new Locale(localeTrimmed[0], localeTrimmed[1]);
                break;
            case 3:
                inputLocale = new Locale(localeTrimmed[0], localeTrimmed[1], localeTrimmed[3]);
                break;
            default:
                throw new IllegalArgumentException();
        }
        if(args[1].length() < 1) {
            throw new IllegalArgumentException();
        }
        Locale outputLocale = new Locale(args[1].split("_")[0]);
        bundle = ResourceBundle.getBundle("ru.ifmo.rain.bobrov.i18n.LocaleResourceBundle", outputLocale);
        final String[] lines;
        try {
            lines = Files.readAllLines(pathIn).toArray(String[]::new);
        } catch (IOException e) {
            System.out.println("I/O error occurred.");
            return;
        }
        TextStatistics statistics = new TextStatistics();
        statistics.initParsers(inputLocale);
        String text = String.join(" ", lines);
        Comparator<ParsedData<String>> stringComparator = ComparatorUtils.getStringLocaledComparator(inputLocale);
        Comparator<ParsedData<Number>> numericComparator = ComparatorUtils.getNumericComparator();
        Results stat = statistics.extractAll(text);
        ArrayList<ParsedData<String>> sentencesData = statistics.splitByIterator(BreakIterator.getSentenceInstance(inputLocale), text, inputLocale);
        ArrayList<ParsedData<String>> linesData = Arrays.stream(lines).map(TextStatistics::createInstanceForLine).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<ParsedData<String>> wordsData = statistics.splitByIterator(BreakIterator.getWordInstance(inputLocale), text, inputLocale);
        try (BufferedWriter writer = Files.newBufferedWriter(pathOut, StandardCharsets.UTF_8)) {
            writer.write("<html><head><meta charset=\"UTF-8\"/></head><body><br>"
                    + System.lineSeparator());
            writer.write("<h1>" + bundle.getString("analyze") + pathIn.getFileName() + "</h1>" + System.lineSeparator());
            writer.write("<h2>" + bundle.getString("stat.all") + "</h2>" + System.lineSeparator());
            writer.write(addBr(bundle.getString("amount." + categories[0]) + " " + sentencesData.size()) + System.lineSeparator());
            writer.write(addBr(bundle.getString("amount." + categories[1]) + " " + linesData.size()) + System.lineSeparator());
            writer.write(addBr(bundle.getString("amount." + categories[2]) + " " + wordsData.size()) + System.lineSeparator());
            writer.write(addBr(bundle.getString("amount." + categories[3]) + " " + stat.getNumbers().size()) + System.lineSeparator());
            writer.write(addBr(bundle.getString("amount." + categories[4]) + " " + stat.getCurrencies().size()) + System.lineSeparator());
            writer.write(addBr(bundle.getString("amount." + categories[5]) + " " + stat.getDates().size()) + System.lineSeparator());
            statistics.printSection(sentencesData, categories[0], stringComparator, writer);
            statistics.printSection(linesData, categories[1], stringComparator, writer);
            statistics.printSection(wordsData, categories[2], stringComparator, writer);
            statistics.printSection(stat.getNumbers(), categories[3], numericComparator, writer);
            statistics.printSection(stat.getCurrencies(), categories[4], numericComparator, writer);
            statistics.printSection(stat.getDates(), categories[5], ComparatorUtils.getDateComparator(), writer);
            writer.write("</body></html>");
        } catch (IOException ex) {
            System.err.println("I/O error occurred.");
        }
    }

    private static String addBr(String str) {
        return str + "<br><br>";
    }

    private <T> void printSection(ArrayList<ParsedData<T>> data, String category, Comparator<ParsedData<T>> comparator, BufferedWriter writer) throws IOException {
        writer.write("<h2>" + bundle.getString(bundleKeys[6] + "." + category) + "</h2>" + System.lineSeparator());
        writer.write("<p>");
        printInfo(data, category, comparator, writer);
    }

    private <T> void printInfo(ArrayList<ParsedData<T>> info, String type,
                               Comparator<ParsedData<T>> comparator, BufferedWriter writer) throws IOException {
        if (info.isEmpty()) {
            writer.write(addBr(bundle.getString("none")) + System.lineSeparator());
            writer.write("</p>" + System.lineSeparator());
            return;
        }
        String[] results = process(info, comparator);
        for (int j = 0; j < 6; j++) {
            writer.write(addBr(bundle.getString(String.format("%s.%s", bundleKeys[j], type)) + results[j]) + System.lineSeparator());
        }
        writer.write("</p>" + System.lineSeparator());
    }

    public <T> String[] process(ArrayList<ParsedData<T>> info, Comparator<ParsedData<T>> comparator) {
        if (info.isEmpty()) return new String[]{"NONE"};
        String[] results = new String[6];
        info.sort(Comparator.comparingInt(s -> s.getParsedString().length()));
        TreeSet<ParsedData<T>> unify = new TreeSet<>(comparator);
        unify.addAll(info);
        String uniqueValue;
        if (unify.size() % 10 == 1 && unify.size() % 100 != 11) {
            uniqueValue = bundle.getString("uniqueSingle");
        } else {
            uniqueValue = bundle.getString("uniqueMulti");
        }
        results[0] = String.format(" %d (%d %s)", info.size(), unify.size(), uniqueValue);
        results[1] = String.format(" %s", toHTML(unify.first().getParsedString()));
        results[2] = String.format(" %s", toHTML(unify.last().getParsedString()));
        results[3] = String.format(" %d (%s)", info.get(0).getParsedString().length(), toHTML(info.get(0).getParsedString()));
        results[4] = String.format(" %d (%s)", info.get(info.size() - 1).getParsedString().length(), toHTML(info.get(info.size() - 1).getParsedString()));
        results[5] = String.format(" %.3f", info.stream().mapToInt(s -> s.getParsedString().length()).average().getAsDouble());
        return results;
    }

    private static String toHTML(String str) {
        StringBuilder builder = new StringBuilder();
        for (Character ch : str.toCharArray()) {
            if (ch == '<') {
                builder.append("&lt");
            } else if (ch == '>') {
                builder.append("&gt");
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public Results extractAll(String text) {
        final ArrayList<ParsedData<Date>> dates = new ArrayList<>();
        final ArrayList<ParsedData<Number>> currencies = new ArrayList<>();
        final ArrayList<ParsedData<Number>> numbers = new ArrayList<>();
        for (ParsePosition pos = new ParsePosition(0); pos.getIndex() < text.length(); ) {
            ParsedData parsedData = tryAll(text, pos.getIndex());
            switch (parsedData.getParsedType()) {
                case DATE:
                    accept(text, pos, parsedData, dates);
                    break;
                case CURRENCY:
                    accept(text, pos, parsedData, currencies);
                    break;
                case NUMBER:
                    accept(text, pos, parsedData, numbers);
                    break;
                case DEFAULT:
                    pos.setIndex(pos.getIndex() + 1);
                    break;
            }
        }
        return new Results(dates, currencies, numbers);
    }

    private <T> void accept(String text, ParsePosition pos, ParsedData<T> data, ArrayList<ParsedData<T>> output) {
        data.attachString(skipWhitespace(text, pos.getIndex(), data.getPos()));
        output.add(data);
        pos.setIndex(data.getPos());
    }

    private String skipWhitespace(String text, int start, int end) {
        while (start < end && Character.isWhitespace(text.codePointAt(start))) {
            start++;
        }
        return text.substring(start, end);
    }

    private ParsedData tryAll(String text, int pos) {
        ParsedData res = tryParseDates(text, pos);
        if (res.getParsedType() != Parsers.DEFAULT) {
            return res;
        }
        res = tryParseNumeric(text, pos, Parsers.CURRENCY);
        if (res.getParsedType() != Parsers.DEFAULT) {
            return res;
        }
        return tryParseNumeric(text, pos, Parsers.NUMBER);
    }

    public ParsedData<Number> tryParseNumeric(String text, int pos, Parsers parsedType) {
        ParsePosition curPos = new ParsePosition(pos);
        Number result = ((NumberFormat) parsers[(parsedType == Parsers.CURRENCY ? 4 : 5)]).parse(text, curPos);
        if (result != null) {
            return new ParsedData<>(parsedType, curPos.getIndex(), result);
        }
        return new ParsedData<>(Parsers.DEFAULT, -1, null);
    }


    public ParsedData<Date> tryParseDates(String text, int pos) {
        for (int i = 0; i < styles.length; i++) {
            ParsePosition curPos = new ParsePosition(pos);
            Date result = ((DateFormat) parsers[i]).parse(text, curPos);
            if (result != null) {
                return new ParsedData<>(Parsers.DATE, curPos.getIndex(), result);
            }
        }
        return new ParsedData<>(Parsers.DEFAULT, -1, null);
    }

    public ArrayList<ParsedData<String>> splitByIterator(BreakIterator iterator, String text, Locale locale) {
        iterator.setText(text);
        ArrayList<ParsedData<String>> output = new ArrayList<>();
        for (int i = iterator.first(), j = iterator.next();
             j != BreakIterator.DONE;
             j = iterator.next()) {
            if (checkBoundary(i, j, text, iterator)) {
                String str = text.substring(i, j).trim();
                ParsedData<String> toAdd = new ParsedData<>(Parsers.DEFAULT, -1, str.toLowerCase(locale));
                toAdd.attachString(str);
                output.add(toAdd);
            }
            i = j;
        }
        return output;
    }

    private boolean checkBoundary(int start, int end, String text, BreakIterator br) {
        if (end - start > 1) {
            return true;
        } else if (end - start == 1 && Character.isLetterOrDigit(text.codePointAt(start))) {
            return true;
        }
        return end == start;
    }

    private static boolean validateArguments(String[] args) {
        return args != null && args.length == 4 && Arrays.stream(args).noneMatch(Objects::isNull);
    }

    private static ParsedData<String> createInstanceForLine(String line) {
        ParsedData<String> result = new ParsedData<>(Parsers.DEFAULT, -1, line);
        result.attachString(line);
        return result;
    }

}
