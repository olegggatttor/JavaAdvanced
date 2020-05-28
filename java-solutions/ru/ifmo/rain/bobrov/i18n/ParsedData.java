package ru.ifmo.rain.bobrov.i18n;

public class ParsedData<T> {
    private final Parsers parsedType;
    private final int pos;
    private final T parsedInfo;
    private String parsedString = "";

    ParsedData(final Parsers parsedType, final int pos, final T parsedInfo) {
        this.parsedType = parsedType;
        this.pos = pos;
        this.parsedInfo = parsedInfo;
    }

    public void attachString(String str) {
        parsedString = str;
    }

    public Parsers getParsedType() {
        return parsedType;
    }

    public int getPos() {
        return pos;
    }

    public T getParsedInfo() {
        return parsedInfo;
    }

    public String getParsedString() {
        return parsedString;
    }
}
