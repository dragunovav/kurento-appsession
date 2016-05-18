package org.kurento.appsession;

import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.internal.server.ServerSession;
import org.slf4j.MDC;

public class SessionMDC {

  private static final String APP_SESSION_ID = "appSessionId";
  private static final String JSON_RPC_SESSION_ID = "jsonRpcSessionId";
  private static final String TRANSPORT_ID = "transportId";
  private static final String PARTICIPANT_ID = "participantId";

  public static void setLogProperties(SessionAppSessionManager clientManager) {
    setLogProperties(clientManager.getSession());
    setLogProperties(clientManager.getAppSession());
    setLogProperties(clientManager.getParticipant());
  }

  public static void setLogProperties(Session session) {
    String transportId = "";
    if (session instanceof ServerSession) {
      transportId = ((ServerSession) session).getTransportId();
    }
    MDC.put(TRANSPORT_ID, transportId);
    MDC.put(JSON_RPC_SESSION_ID, session.getSessionId());
  }

  public static void setLogProperties(Participant participant) {
    if (participant != null) {
      MDC.put(PARTICIPANT_ID, participant.getId());
    }
  }

  public static void setLogProperties(AppSession<?> appSession) {
    if (appSession != null) {
      MDC.put(APP_SESSION_ID, appSession.getId());
    }
  }

  public static void removeLogProperties() {
    MDC.remove(TRANSPORT_ID);
    MDC.remove(JSON_RPC_SESSION_ID);
    MDC.remove(APP_SESSION_ID);
    MDC.remove(PARTICIPANT_ID);
  }

}
