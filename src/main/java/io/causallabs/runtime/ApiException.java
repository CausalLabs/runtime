package io.causallabs.runtime;

/** bad call to the server */
public class ApiException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 6621727584733885748L;

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String string, Exception e) {
        super(string, e);
    }


}
