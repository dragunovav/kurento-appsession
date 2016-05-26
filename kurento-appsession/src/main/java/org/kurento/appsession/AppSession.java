package org.kurento.appsession;

import java.util.Collection;
import java.util.Set;

public interface AppSession<P extends Participant<?, ?>> {

  public String getId();

  public void setId(String appSessionId);

  public boolean isReleased();

  public int getNumParticipants();

  public Collection<P> getParticipants();

  public Set<String> getParticipantIds();

  public P removeParticipant(P participant, boolean gracefully) throws InterruptedException;

  public void addParticipant(P participant);

  public void release() throws InterruptedException;

  public P getParticipant(String senderId);

}
