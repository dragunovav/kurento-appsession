package org.kurento.appsession;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import org.kurento.appsession.rmi.RemoteParticipant;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.internal.ws.WebSocketServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

public abstract class AppSessionManager<S extends AppSession<P>, P extends Participant<R>, R extends RemoteParticipant> {

  private final Logger log = LoggerFactory.getLogger(AppSessionManager.class);

  @Autowired
  private ApplicationContext context;

  private final ConcurrentHashMap<String, S> appSessions = new ConcurrentHashMap<>();

  private Class<S> appSessionClass;

  public void setAppSessionClass(Class<S> appSessionClass) {
    this.appSessionClass = appSessionClass;
  }

  public S getAppSession(String appSessionId) {

    return appSessions.computeIfAbsent(appSessionId, id -> {

      S appSession;

      try {

        // context.getBean(name, requiredType)

        appSession = appSessionClass.newInstance();
        appSession.setId(id);

        // TODO Review if appSession can be created as bean

        context.getAutowireCapableBeanFactory().autowireBeanProperties(appSession,
            AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);

        execPostConstructMethod(appSession);

        return appSession;

      } catch (Exception e) {
        throw new AppSessionException("Exception creating a new object of AppSession class "
            + appSessionClass.getClass().getName());
      }

    });
  }

  private void execPostConstructMethod(AppSession appSession) {
    for (Method m : appSession.getClass().getMethods()) {
      if (m.getAnnotation(PostConstruct.class) != null) {
        try {
          m.invoke(appSession);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          log.error("Excepcion executing @PostConstruct method in appSession {}", appSession);
        }
      }
    }
  }

  public void afterConnectionEstablished(SessionAppSessionManager sessionAppSessionManager,
      Session session) throws Exception {
  }

  @SuppressWarnings("unchecked")
  public void afterConnectionClosed(SessionAppSessionManager sessionAppSessionManager,
      Session session, String status) throws Exception {

    S appSession = (S) sessionAppSessionManager.getAppSession();

    if (appSession != null) {

      P participant = (P) sessionAppSessionManager.getParticipant();

      try {

        log.info(
            "Removing participant {} from session {} because jsonRpcSession {} with transportId {} is closed",
            participant.getId(), appSession.getId(), session.getSessionId(),
            getTransportId(session));

        appSession.removeParticipant(participant, false);

      } catch (Exception e) {

        log.error(
            "Exception removing participant {} from app session {}. Possible appSession 'leak'",
            participant.getId(), appSession.getId(), e);

      } finally {
        if (appSession.getNumParticipants() == 0) {

          log.info("Removing appSession {} because all participants are gone", appSession.getId());

          appSessions.remove(appSession.getId());
        }
      }

    } else {
      log.info(
          "Closing websocket connection with transportId={} without AppSession associated to it",
          getTransportId(session));
    }
  }

  private String getTransportId(Session session) {
    if (session instanceof WebSocketServerSession) {
      return ((WebSocketServerSession) session).getTransportId();
    } else {
      return "clustered";
    }
  }

  public void handleTransportError(SessionAppSessionManager appSessionManager, Session session,
      Throwable exception) throws Exception {

    log.warn("Transport error. Exception " + exception.getClass().getName() + ":"
        + exception.getLocalizedMessage());
  }

  public void handleUncaughtException(SessionAppSessionManager appSessionManager, Session session,
      Exception exception) {
    log.warn("Uncaught exception in handler " + this.getClass().getName(), exception);
  }

  public ConcurrentMap<String, S> getAppSessions() {
    return appSessions;
  }

  public AppSession<P> remove(String sessionId) {
    return appSessions.remove(sessionId);
  }

}
