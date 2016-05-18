package org.kurento.appsession.sample;

import org.kurento.appsession.rmi.JsonRpcNotification;
import org.kurento.appsession.rmi.RemoteParticipant;

/**
 * This class contains requests that can be made from server to client. An object from this class
 * can be obtained in the Participant class. A method call in this object is converted to a request
 * to participant client. In this case, there are only an operation "req" and it is a notification.
 */
public interface SampleRemoteParticipant extends RemoteParticipant {

  @JsonRpcNotification
  public void req();

}