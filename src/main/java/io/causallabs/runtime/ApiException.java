package io.causallabs.runtime;

/** bad call to the server */
public class ApiException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 6621727584733885748L;

    public ApiException(int statusCode, String message) {
        super(message);
        m_statusCode = statusCode;
    }

    public ApiException(String message) {
        this(400, message);
    }

    public ApiException(int statusCode, String string, Throwable e) {
        super(string, e);
        m_statusCode = statusCode;
    }

    public ApiException(String string, Throwable e) {
        this(400, string, e);
    }

    public int getStatusCode() {
        return m_statusCode;
    }

    private final int m_statusCode;

}
