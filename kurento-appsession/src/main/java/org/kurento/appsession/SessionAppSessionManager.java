package org.kurento.appsession;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kurento.appsession.rmi.DynamicRemoteParticipantCreator;
import org.kurento.appsession.rmi.RemoteParticipant;
import org.kurento.jsonrpc.Session;

public class SessionAppSessionManager {

  private static final String CLIENT_MANAGER_ATT = "clientManager";

  private AppSession<?> appSession;
  private Participant participant;
  private RemoteParticipant remote;
  private Session session;

  // TODO Make this configurable
  private static ExecutorService exec = Executors.newFixedThreadPool(10);

  public static SessionAppSessionManager getOrCreateFromSession(Session session,
      Class<? extends RemoteParticipant> remoteParticipantClass) {

    if (session == null) {
      return null;
    }

    SessionAppSessionManager clientManager = (SessionAppSessionManager) session.getAttributes()
        .get(CLIENT_MANAGER_ATT);
    if (clientManager == null) {
      clientManager = new SessionAppSessionManager(session,
          DynamicRemoteParticipantCreator.createFor(session, remoteParticipantClass, exec));
      session.getAttributes().put(CLIENT_MANAGER_ATT, clientManager);
    }

    return clientManager;
  }

  public static boolean hasInSession(Session session) {
    return session.getAttributes().get(CLIENT_MANAGER_ATT) != null;
  }

  public static SessionAppSessionManager getFromSession(Session session) {
    return (SessionAppSessionManager) session.getAttributes().get(CLIENT_MANAGER_ATT);
  }

  public static void removeAppSession(Session session) {
    getFromSession(session).removeFromAppSession();
  }

  public SessionAppSessionManager(Session session, RemoteParticipant remote) {
    this.remote = remote;
    this.session = session;
  }

  public AppSession<?> getAppSession() {
    return appSession;
  }

  public boolean hasAppSession() {
    return appSession != null;
  }

  public Participant getParticipant() {
    return participant;
  }

  public RemoteParticipant getRemote() {
    return remote;
  }

  public void setParticipant(Participant participant) {
    this.participant = participant;
    this.appSession = participant.getAppSession();
  }

  public void removeFromAppSession() {
    this.participant = null;
    this.appSession = null;
  }

  public void setRemote(RemoteParticipant remote) {
    this.remote = remote;
  }

  public Session getSession() {
    return session;
  }

}