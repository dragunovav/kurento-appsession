package org.kurento.appsession.sample;

import org.kurento.appsession.DefaultAppSession;
import org.kurento.appsession.rmi.AppSessionId;
import org.kurento.appsession.rmi.JsonRpcMethod;
import org.kurento.jsonrpc.Session;

public class SampleAppSession
    extends DefaultAppSession<SampleParticipant, SampleRemoteParticipant> {

  private int counter = 0;

  @JsonRpcMethod
  public String login(@AppSessionId String appSessionId, Session session) {
    return "OK";
  }

  @JsonRpcMethod
  public int incCounter() {
    counter++;
    return counter;
  }

  @JsonRpcMethod
  public int getCounter() {
    return counter;
  }

  @JsonRpcMethod
  public void reverse(SampleRemoteParticipant remote) {
    remote.req();
  }
}