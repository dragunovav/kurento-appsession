package org.kurento.appsession;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.appsession.rmi.RemoteParticipant;

public class DefaultAppSession<P extends Participant<R>, R extends RemoteParticipant>
    implements AppSession<P> {

  private String id;

  private ConcurrentMap<String, P> participants = new ConcurrentHashMap<>();

  protected volatile boolean released = false;

  public void setId(String id) {
    this.id = id;
  }

  public final String getId() {
    return id;
  }

  public boolean isReleased() {
    return this.released;
  }

  @Override
  public int getNumParticipants() {
    return participants.size();
  }

  @Override
  public Collection<P> getParticipants() {
    return Collections.unmodifiableCollection(participants.values());
  }

  @Override
  public Set<String> getParticipantIds() {
    return Collections.unmodifiableSet(participants.keySet());
  }

  @Override
  public void addParticipant(P participant) {

    checkReleased();

    P oldParticipant = participants.putIfAbsent(participant.getId(), participant);
    if (oldParticipant != null) {
      throw new AppSessionException(
          "Participant " + participant.getId() + " already included in appSession " + getId());
    }
  }

  @Override
  public P removeParticipant(P participant, boolean gracefully) {

    checkReleased();

    return participants.remove(participant.getId());
  }

  @Override
  public void release() {
    participants.values().forEach(p -> p.release());
    released = true;
  }

  private void checkReleased() {
    if (released) {
      throw new AppSessionException("Trying to remove a participant from appSession " + getId()
          + " but it is already released");
    }
  }

  protected R sendToAll() {
    throw new UnsupportedOperationException("SendToAll is not yet implemented");
  }

  protected R sendToOthers() {
    throw new UnsupportedOperationException("SendToOthers is not yet implemented");
  }

  @Override
  public P getParticipant(String participantId) {
    return participants.get(participantId);
  }

}