package org.kurento.appsession;

import org.kurento.appsession.rmi.RemoteParticipant;
import org.kurento.jsonrpc.Session;

public interface Participant<R extends RemoteParticipant> {

  String getId();

  AppSession<?> getAppSession();

  void setAppSession(AppSession<?> appSession);

  Session getSession();

  void setSession(Session session);

  void release();

  boolean testIfConnected();

  public R send();

  void setRemote(R remote);

}