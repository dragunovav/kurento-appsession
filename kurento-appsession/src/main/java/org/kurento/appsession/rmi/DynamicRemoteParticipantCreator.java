package org.kurento.appsession.rmi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import org.kurento.client.internal.ParamAnnotationUtils;
import org.kurento.client.internal.client.DefaultInvocationHandler;
import org.kurento.jsonrpc.JsonRpcException;
import org.kurento.jsonrpc.Props;
import org.kurento.jsonrpc.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class DynamicRemoteParticipantCreator {

  private static Logger log = LoggerFactory.getLogger(DynamicRemoteParticipantCreator.class);

  public static class DynamicRemoteParticipantHandler extends DefaultInvocationHandler {

    private Session session;
    private static Gson gson = new GsonBuilder().create();

    private ExecutorService exec;

    public DynamicRemoteParticipantHandler(Session session, ExecutorService exec) {
      this.session = session;
      this.exec = exec;
    }

    @Override
    public Object internalInvoke(Object proxy, Method method, Object[] args) throws Throwable {

      final String methodName = method.getName();

      try {

        final Props params = ParamAnnotationUtils.extractProps(
            Arrays.asList(JsonRpcAndJavaMethodManager.lookupParameterNames(method)), args);

        if (method.isAnnotationPresent(JsonRpcNotification.class)) {

          exec.submit(new Runnable() {
            @Override
            public void run() {
              try {
                session.sendNotification(methodName, params);
              } catch (Exception e) {
                log.debug(
                    "Exception '{}' while sending notification to remote participant: methodName={}, params={}. Possibly the participant is gone. Closing session",
                    e.getClass().getName() + ":" + e.getMessage(), methodName, params);
                try {
                  session.close();
                } catch (IOException e1) {
                  log.warn("Exception '{}' closing session",
                      e.getClass().getName() + ":" + e.getMessage(), e);
                }
              }
            }
          });

          return null;

        } else {
          JsonElement result = session.sendRequest(methodName, params);

          return gson.fromJson(result, method.getReturnType());
        }

      } catch (IOException e) {
        // TODO Use an appropriate exception class
        throw new JsonRpcException(
            "Exception executing method '" + methodName + "' in remote participant", e);
      }
    }
  }

  public static RemoteParticipant createFor(Session session,
      Class<? extends RemoteParticipant> remoteParticipantClass, ExecutorService exec) {

    DynamicRemoteParticipantHandler handler = new DynamicRemoteParticipantHandler(session, exec);

    return (RemoteParticipant) Proxy.newProxyInstance(remoteParticipantClass.getClassLoader(),
        new Class[] { remoteParticipantClass }, handler);
  }
}
