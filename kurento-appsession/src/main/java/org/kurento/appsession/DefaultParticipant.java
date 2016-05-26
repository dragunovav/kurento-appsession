package org.kurento.appsession;

import java.util.UUID;

import org.kurento.appsession.rmi.RemoteParticipant;
import org.kurento.jsonrpc.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultParticipant<A extends AppSession<?>, R extends RemoteParticipant>
    implements Participant<A, R> {

  private static final Logger log = LoggerFactory.getLogger(DefaultParticipant.class);

  private String id;
  protected A appSession;
  protected Session session;
  private R remote;

  public DefaultParticipant() {
    this.id = UUID.randomUUID().toString();
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(String participantId) {
    this.id = participantId;
  }

  @Override
  public A getAppSession() {
    return appSession;
  }

  @Override
  public void setAppSession(A appSession) {
    this.appSession = appSession;
  }

  @Override
  public Session getSession() {
    return session;
  }

  @Override
  public void setSession(Session session) {
    this.session = session;
  }

  public void setRemote(R remote) {
    this.remote = remote;
  }

  @Override
  public void release() {
    try {
      SessionAppSessionManager.removeAppSession(session);
      // TODO: Review the impact of closing the WS session in all cases.
      // this.session.close();
    } catch (Exception e) {
      log.warn("Exception removing session object from websocket connection", e);
    }
  }

  @Override
  public boolean testIfConnected() {
    try {
      session.sendRequest("pull");
      return true;
    } catch (Exception e) {
      try {
        session.close();
      } catch (Exception e2) {
        log.warn("Exception closing jsonRpc session {} associated to participant {} in session {}",
            session.getSessionId(), this.getId(), this.getAppSession().getId());
      }
      return false;
    }
  }

  @Override
  public R send() {
    return remote;
  }

}
