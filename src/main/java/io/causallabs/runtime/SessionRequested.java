package io.causallabs.runtime;

public interface SessionRequested extends Requested {
    ///////////////////////////////////////////////////////////////////////////////
    // Support for not logging out a session (because of bots or another reason)
    ///////////////////////////////////////////////////////////////////////////////
    public void setNoLog(boolean b);

    public boolean isNoLog();
}
