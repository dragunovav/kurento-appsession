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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.kurento.appsession.DefaultAppSession;
import org.kurento.appsession.rmi.AppSessionId;
import org.kurento.appsession.rmi.JsonRpcMethod;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class Room extends DefaultAppSession<RoomParticipant, RoomRemoteParticipant> {

  private static final Logger log = LoggerFactory.getLogger(Room.class);

  @Autowired
  private KurentoClient kurento;

  private MediaPipeline pipeline;

  @PostConstruct
  public void init() {

    this.pipeline = kurento.createMediaPipeline();

    log.debug("ROOM {} has been created", getId());
  }

  @PreDestroy
  private void shutdown() {
    this.release();
  }

  @JsonRpcMethod
  public List<String> joinRoom(@AppSessionId String roomId, String participantId,
      RoomParticipant newParticipant) {

    log.debug("ROOM {}: adding participant {}", roomId, participantId);

    newParticipant.setPipeline(pipeline);
    newParticipant.setId(participantId);

    addParticipant(newParticipant);

    // sendToOthers().newParticipantArrived(newParticipant.getId());
    for (RoomParticipant p : getParticipants()) {
      if (p != newParticipant) {
        p.send().newParticipantArrived(participantId);
      }
    }

    List<String> otherParticipants = new ArrayList<>(getParticipantIds());
    otherParticipants.remove(participantId);

    return otherParticipants;
  }

  @JsonRpcMethod
  public void leaveRoom(RoomParticipant participant) {

    log.debug("PARTICIPANT {}: Leaving room {}", participant.getId(), getId());

    String participantId = participant.getId();

    // TODO This should be handled by appsession
    removeParticipant(participant, true);
    participant.release();

    log.debug("ROOM {}: notifying all users that {} is leaving the room", getId(), participantId);

    // sendToAll().participantLeft(name);
    for (RoomParticipant p : getParticipants()) {
      p.send().participantLeft(participantId);
    }
  }

  @Override
  public void release() {

    super.release();

    log.debug("Room {} closed", getId());

    pipeline.release(ReleaseContinuation.create("pipeline for room " + getId()));

  }
}
