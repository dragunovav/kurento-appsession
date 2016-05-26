package org.kurento.appsession;

import org.kurento.appsession.rmi.RemoteParticipant;
import org.kurento.jsonrpc.Session;

public interface Participant<A extends AppSession<?>, R extends RemoteParticipant> {

  String getId();

  A getAppSession();

  void setAppSession(A appSession);

  Session getSession();

  void setSession(Session session);

  void release();

  boolean testIfConnected();

  public R send();

  void setRemote(R remote);

}