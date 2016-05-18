package org.kurento.appsession.basicroom;

import org.kurento.appsession.rmi.JsonRpcNotification;
import org.kurento.appsession.rmi.RemoteParticipant;
import org.kurento.client.IceCandidate;

public interface RoomRemoteParticipant extends RemoteParticipant {

  @JsonRpcNotification
  void iceCandidate(String participantId, IceCandidate iceCandidate);

  @JsonRpcNotification
  void newParticipantArrived(String participantId);

  @JsonRpcNotification
  void participantLeft(String participantId);

}
