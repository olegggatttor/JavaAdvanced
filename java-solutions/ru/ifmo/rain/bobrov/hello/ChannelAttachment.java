package ru.ifmo.rain.bobrov.hello;

/**
 * Class is used to store channels information.
 */
class ChannelAttachment {
    private final int amountOfRequests;
    private int requestPosition;
    private final String str;

    ChannelAttachment(final String prefix, final int pos, final int requests) {
        amountOfRequests = requests;
        str = prefix + pos + "_";
    }

    /**
     * @return result string.
     */
    String getCurrentString() {
        return (str + requestPosition);
    }

    /**
     *  Increases current request.
     */
    void increaseRequests() {
        requestPosition++;
    }

    /**
     *
     * @return true if all requests are sent and false otherwise.
     */
    boolean isDone() {
        return requestPosition == (amountOfRequests);
    }
}
