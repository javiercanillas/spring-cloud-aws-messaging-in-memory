package io.github.javiercanillas.amazonws.services.sqs;

public class NotImplementedException extends RuntimeException {

    private static final long serialVersionUID = -7721520252685664625L;

    /**
     * Constructs a NotImplementedException.
     *
     * @param message description of the exception
     * @since 3.2
     */
    public NotImplementedException(final String message) {
        super(message);
    }

}
