package org.kurento.appsession;

public class AppSessionException extends RuntimeException {

  private static final long serialVersionUID = -4190611190323648402L;

  public AppSessionException() {
  }

  public AppSessionException(String message) {
    super(message);
  }

  public AppSessionException(Throwable cause) {
    super(cause);
  }

  public AppSessionException(String message, Throwable cause) {
    super(message, cause);
  }

  public AppSessionException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
