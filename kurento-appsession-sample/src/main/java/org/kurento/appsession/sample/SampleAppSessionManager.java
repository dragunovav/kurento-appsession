package org.kurento.appsession.sample;

import org.kurento.appsession.AppSessionManager;
import org.kurento.appsession.rmi.JsonRpcMethod;

public class SampleAppSessionManager
    extends AppSessionManager<SampleAppSession, SampleParticipant, SampleRemoteParticipant> {

  @JsonRpcMethod
  public String nonAppSessionMethod(String param) {
    return param;
  }

}