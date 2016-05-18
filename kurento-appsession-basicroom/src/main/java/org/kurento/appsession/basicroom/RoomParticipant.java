/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.appsession.basicroom;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.appsession.DefaultParticipant;
import org.kurento.appsession.rmi.JsonRpcMethod;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomParticipant extends DefaultParticipant<RoomRemoteParticipant> {

  private static final Logger log = LoggerFactory.getLogger(RoomParticipant.class);

  private MediaPipeline pipeline;

  private WebRtcEndpoint incomingEP;
  private ConcurrentMap<String, WebRtcEndpoint> outgoingEPs = new ConcurrentHashMap<>();

  public void setPipeline(MediaPipeline pipeline) {

    this.pipeline = pipeline;

    this.incomingEP = new WebRtcEndpoint.Builder(pipeline).build();

    this.incomingEP
        .addOnIceCandidateListener(event -> send().iceCandidate(getId(), event.getCandidate()));
  }

  public WebRtcEndpoint getIncomingEP() {
    return incomingEP;
  }

  public Room getRoom() {
    return (Room) getAppSession();
  }

  @JsonRpcMethod
  public String negotiateMediaFor(String participantId, String sdpOffer) throws IOException {

    log.info("PARTICIPANT {}: connecting with {} in room {}", getId(), participantId,
        getRoom().getId());

    WebRtcEndpoint mediaEP;

    if (participantId.equals(getId())) {
      log.debug("PARTICIPANT {}: configuring incoming media", getId());
      mediaEP = incomingEP;
    } else {
      mediaEP = this.createOutgoingEPFor(participantId);
    }

    String ipSdpAnswer = mediaEP.processOffer(sdpOffer);
    mediaEP.gatherCandidates();

    log.trace("PARTICIPANT {}: SdpAnswer for {} is {}", getId(), participantId, ipSdpAnswer);

    return ipSdpAnswer;
  }

  private WebRtcEndpoint createOutgoingEPFor(String participantId) {

    log.debug("PARTICIPANT {}: receiving video from {}", getId(), participantId);

    WebRtcEndpoint outgoingEP = new WebRtcEndpoint.Builder(pipeline).build();
    outgoingEPs.put(participantId, outgoingEP);

    WebRtcEndpoint participantIncomingEP = getRoom().getParticipant(participantId).getIncomingEP();

    participantIncomingEP.connect(outgoingEP);

    outgoingEP.addOnIceCandidateListener(
        event -> send().iceCandidate(participantId, event.getCandidate()));

    return outgoingEP;
  }

  public void cancelVideoFrom(String participantId) {

    log.debug("PARTICIPANT {}: canceling video reception from {}", this.getId(), participantId);

    WebRtcEndpoint outgoingEP = this.outgoingEPs.remove(participantId);

    outgoingEP.release(
        ReleaseContinuation.create("outgoingEP for " + participantId + " in " + this.getId()));
  }

  @JsonRpcMethod
  public void onRemoteIceCandidate(IceCandidate iceCandidate, String participantId) {

    if (getId().equals(participantId)) {

      incomingEP.addIceCandidate(iceCandidate);

    } else {

      WebRtcEndpoint outgoingEP = outgoingEPs.get(participantId);

      if (outgoingEP != null) {
        outgoingEP.addIceCandidate(iceCandidate);
      } else {
        log.warn("Received an iceCandidate for non-existing participant {}", participantId);
      }
    }
  }

  @Override
  public void release() {

    super.release();

    log.debug("PARTICIPANT {}: Releasing resources", getId());

    for (final String remoteParticipantName : outgoingEPs.keySet()) {

      WebRtcEndpoint ep = this.outgoingEPs.get(remoteParticipantName);

      ep.release(ReleaseContinuation
          .create("incoming EP for " + remoteParticipantName + " in " + getId()));
    }

    incomingEP.release(ReleaseContinuation.create("outgoing EP for " + getId()));
  }

}
