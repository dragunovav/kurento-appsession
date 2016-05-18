package org.kurento.appsession.basicroom;

import org.kurento.client.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReleaseContinuation {

  public static final Logger log = LoggerFactory.getLogger(ReleaseContinuation.class);

  public static Continuation<Void> create(String message) {

    return new Continuation<Void>() {

      @Override
      public void onSuccess(Void result) throws Exception {
        log.trace("Successfully released {}");
      }

      @Override
      public void onError(Throwable cause) throws Exception {
        log.warn("Error releasing {}", cause);
      }
    };
  }
}
