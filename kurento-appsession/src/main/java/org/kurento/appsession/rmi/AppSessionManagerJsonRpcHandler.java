package org.kurento.appsession.rmi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.kurento.appsession.AppSession;
import org.kurento.appsession.AppSessionIdExtractor;
import org.kurento.appsession.AppSessionManager;
import org.kurento.appsession.Participant;
import org.kurento.appsession.SessionAppSessionManager;
import org.kurento.appsession.SessionMDC;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class AppSessionManagerJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject>
    implements AppSessionIdExtractor {

  private static final Logger log = LoggerFactory.getLogger(AppSessionManagerJsonRpcHandler.class);

  private AppSessionManager<?, ?, ?> appSessionManager;

  private Map<String, Method> appSessionManagerMethods;
  private Map<String, Method> appSessionMethods;
  private Map<String, Method> participantMethods;

  private Class<? extends AppSession<?>> appSessionClass;
  private Class<? extends Participant<?, ?>> participantClass;
  private Class<? extends RemoteParticipant> remoteParticipantClass;

  private static JsonRpcAndJavaMethodManager methodManager = new JsonRpcAndJavaMethodManager();

  @SuppressWarnings("unchecked")
  public <A extends AppSession<P>, P extends Participant<A, R>, R extends RemoteParticipant> AppSessionManagerJsonRpcHandler(
      AppSessionManager<A, P, R> appSessionManager) {

    this.appSessionManager = appSessionManager;
    this.appSessionManagerMethods = loadJsonRpcMethods(this.appSessionManager.getClass());

    Class<?>[] typeParams = getTypeParams(appSessionManager.getClass());

    this.appSessionClass = (Class<? extends AppSession<?>>) typeParams[0];
    this.participantClass = (Class<? extends Participant<?, ?>>) typeParams[1];
    this.remoteParticipantClass = (Class<? extends RemoteParticipant>) typeParams[2];

    appSessionManager.setAppSessionClass((Class<A>) appSessionClass);

    this.appSessionMethods = loadJsonRpcMethods(appSessionClass);

    this.participantMethods = loadJsonRpcMethods(participantClass);
  }

  @Override
  public void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {

    Session session = transaction.getSession();

    try {

      SessionAppSessionManager clientManager = getSessionAppSessionManager(session);

      SessionMDC.setLogProperties(clientManager);

      log.info("Req -> " + request);

      String methodName = request.getMethod();

      Method m = appSessionManagerMethods.get(methodName);
      if (m != null) {

        log.info("Executing non-session method '" + methodName + "'");

        methodManager.executeMethod(clientManager, appSessionManager, m, transaction, request);

      } else {

        log.info("Executing session method '" + methodName + "'");

        m = appSessionMethods.get(methodName);

        if (m != null) {

          AppSession<?> appSession = clientManager.getAppSession();

          if (appSession == null) {

            processAppSessionRequestWithoutAppSession(clientManager, m, transaction, request);

          } else {

            methodManager.executeMethod(clientManager, appSession, m, transaction, request);

          }

        } else {

          m = participantMethods.get(methodName);

          if (m != null) {

            Participant<?,?> participant = clientManager.getParticipant();

            methodManager.executeMethod(clientManager, participant, m, transaction, request);

          } else {

            String errorMsg = "Method '" + methodName + "' doesn't exist in '"
                + this.appSessionManager.getClass().getName() + "' or '" + appSessionClass.getName()
                + "' or '" + participantClass.getClass().getName() + "'";

            log.error(errorMsg);
            transaction.sendError(11, "METHOD_NOT_FOUND", errorMsg, null);

          }
        }
      }
    } finally {
      SessionMDC.removeLogProperties();
    }
  }

  private void processAppSessionRequestWithoutAppSession(SessionAppSessionManager clientManager,
      Method m, Transaction transaction, Request<JsonObject> request) throws IOException {

    // Extract appSessionId from request
    String appSessionId = extractAppSessionIdFromRequest(request);

    if (appSessionId == null) {

      String errorMsg = "Trying to execute the appSession method '" + m.getName()
          + "' without appSession bound and this method has no @AppSessionId annotation in any parameter";

      log.error(errorMsg);
      transaction.sendError(10, "NON_SESSION_ERROR", errorMsg, null);
      return;
    }

    AppSession<?> appSession = appSessionManager.getAppSession(appSessionId);

    // Create participant
    try {

      Participant p = this.participantClass.newInstance();

      p.setSession(transaction.getSession());
      p.setRemote(clientManager.getRemote());

      clientManager.setParticipant(p);

      // Execute operation
      boolean success = methodManager.executeMethod(clientManager, appSession, m, transaction,
          request);

      if (!success) {
        clientManager.setParticipant(null);

      } else {
        // If no exception associate appSession to participant
        // addParticipantToAppSession(appSession, p);
        p.setAppSession(appSession);
        clientManager.setParticipant(p);
      }

    } catch (InstantiationException | IllegalAccessException e) {

      String errorMsg = "Exception creating new instance of class " + participantClass.getName()
          + " for participant. This class must be public and have a non-args public constructor.";

      log.error(errorMsg, e);
      transaction.sendError(10, "PARTICIPANT_CREATION_ERROR", errorMsg, null);

    }
  }

  // @SuppressWarnings({ "unchecked", "rawtypes" })
  // private void addParticipantToAppSession(AppSession<?> appSession, Participant p) {
  // ((AppSession) appSession).addParticipant(p);
  // }

  private SessionAppSessionManager getSessionAppSessionManager(Session session) {
    return SessionAppSessionManager.getOrCreateFromSession(session, remoteParticipantClass);
  }

  private Map<String, Method> loadJsonRpcMethods(Class<?> clazz) {
    Map<String, Method> methods = new HashMap<>();
    for (Method m : clazz.getMethods()) {
      if (m.isAnnotationPresent(JsonRpcMethod.class)) {
        methods.put(m.getName(), m);
      }
    }
    return methods;
  }

  @Override
  public void afterConnectionEstablished(Session session) throws Exception {
    appSessionManager.afterConnectionEstablished(getSessionAppSessionManager(session), session);
  }

  @Override
  public void afterConnectionClosed(Session session, String status) throws Exception {
    appSessionManager.afterConnectionClosed(getSessionAppSessionManager(session), session, status);
  }

  @Override
  public void handleTransportError(Session session, Throwable exception) throws Exception {
    appSessionManager.handleTransportError(getSessionAppSessionManager(session), session,
        exception);
  }

  @Override
  public void handleUncaughtException(Session session, Exception exception) {
    appSessionManager.handleUncaughtException(getSessionAppSessionManager(session), session,
        exception);
  }

  private Class<?>[] getTypeParams(Class<?> sessionManagerClass) {

    Type genericSuperclass = sessionManagerClass.getGenericSuperclass();

    while (true) {
      if (genericSuperclass instanceof ParameterizedType) {

        ParameterizedType parameterized = (ParameterizedType) genericSuperclass;

        if (parameterized.getRawType() == AppSessionManager.class) {

          Type[] types = parameterized.getActualTypeArguments();
          Class<?>[] params = new Class<?>[types.length];

          for (int i = 0; i < params.length; i++) {
            params[i] = (Class<?>) types[i];
          }

          return params;
        } else {
          break;
        }

      } else if (genericSuperclass instanceof Class) {
        genericSuperclass = ((Class<?>) genericSuperclass).getGenericSuperclass();
      } else {
        break;
      }
    }

    throw new IllegalArgumentException(
        "Unable to obtain type parameters from class " + sessionManagerClass);
  }

  @Override
  public String extractAppSessionIdFromRequest(Request<JsonObject> request) {

    Method method = appSessionMethods.get(request.getMethod());

    if (method == null) {
      log.debug("Method {} not present in appSession class {}", request.getMethod(),
          appSessionClass.getName());
      return null;
    }

    String appSessionIdParamName = null;

    String[] parameterNames = JsonRpcAndJavaMethodManager.lookupParameterNames(method);

    int i = 0;
    for (Parameter p : method.getParameters()) {
      AppSessionId ann = p.getAnnotation(AppSessionId.class);
      if (ann != null) {
        appSessionIdParamName = parameterNames[i];
        break;
      }
      i++;
    }

    if (appSessionIdParamName == null) {
      log.debug("Method {} in appSession class {} has no parameter with @AppSessionId",
          method.getName(), appSessionClass.getName());
      return null;
    }

    return request.getParams().get(appSessionIdParamName).getAsString();
  }
}
