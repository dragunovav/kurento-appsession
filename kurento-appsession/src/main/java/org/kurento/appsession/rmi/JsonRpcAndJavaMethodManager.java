package org.kurento.appsession.rmi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.kurento.appsession.AppSession;
import org.kurento.appsession.AppSessionException;
import org.kurento.appsession.DefaultParticipant;
import org.kurento.appsession.SessionAppSessionManager;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.ParameterNamesNotFoundException;

public class JsonRpcAndJavaMethodManager {

  private static final Logger log = LoggerFactory.getLogger(JsonRpcAndJavaMethodManager.class);

  private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public boolean executeMethod(SessionAppSessionManager clientManager, Object object, Method m,
      Transaction transaction, Request<JsonObject> request) throws IOException {

    try {

      Response<JsonElement> response = execJavaMethod(clientManager, object, m, transaction,
          request);

      if (response != null) {
        response.setId(request.getId());
        transaction.sendResponseObject(response);
      } else {
        transaction.sendVoidResponse();
      }

    } catch (InvocationTargetException e) {

      if (e.getCause() instanceof JsonRpcErrorException) {

        JsonRpcErrorException ex = (JsonRpcErrorException) e.getCause();

        transaction.sendError(ex.getError());

      } else {

        log.error(
            "Exception executing request " + request + ": " + e.getCause().getLocalizedMessage(),
            e.getCause());
        transaction.sendError(e.getCause());

        return false;
      }

    } catch (Exception e) {
      log.error("Exception processing request " + request, e);
      transaction.sendError(e);
      return false;
    }

    return true;

  }

  private Response<JsonElement> execJavaMethod(SessionAppSessionManager clientManager,
      Object object, Method m, Transaction transaction, Request<JsonObject> request)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

    Object[] values = calculateParamValues(clientManager, m, request);

    Object result = m.invoke(object, values);

    if (result == null) {
      return null;
    } else {
      return new Response<>(null, gson.toJsonTree(result));
    }
  }

  private Object[] calculateParamValues(SessionAppSessionManager clientManager, Method m,
      Request<JsonObject> request) {

    JsonObject params = request.getParams();

    String[] parameterNames = lookupParameterNames(m);
    Type[] parameterTypes = m.getGenericParameterTypes();

    Object[] values = new Object[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {

      values[i] = getValueFromParam(clientManager, m, params, parameterNames[i], parameterTypes[i]);
    }

    if (log.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < parameterNames.length; i++) {
        sb.append(parameterNames[i] + "(" + parameterTypes[i] + ")=" + values[i] + ",");
      }
      sb.append("]");

      log.info("Executing method {} with params {}", m.getName(), params);
    }
    return values;
  }

  public static String[] lookupParameterNames(Method m) {

    Parameter[] parameters = m.getParameters();
    String[] names = new String[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      names[i] = parameters[i].getName();
    }

    if (names.length > 0 && names[0].equals("arg0")) {
      try {
        names = new BytecodeReadingParanamer().lookupParameterNames(m, true);
      } catch (ParameterNamesNotFoundException e) {
        throw new AppSessionException("Parameter names not found in class files. "
            + "Compile class with debug information or " + "use Java 8 -parameter compiler option");
      }
    }

    return names;
  }

  private Object getValueFromParam(SessionAppSessionManager clientManager, Method m,
      JsonObject params, String parameterName, Type genericType) {

    if (genericType instanceof Class) {

      Class<?> type = (Class<?>) genericType;

      if (DefaultParticipant.class.isAssignableFrom(type)) {
        return clientManager.getParticipant();
      } else if (RemoteParticipant.class.isAssignableFrom(type)) {
        return clientManager.getRemote();
      } else if (AppSession.class.isAssignableFrom(type)) {
        return clientManager.getAppSession();
      } else if (Session.class.isAssignableFrom(type)) {
        return clientManager.getSession();

      } else {

        // TODO Allow more types
        JsonElement jsonElement = params.get(parameterName);

        if (jsonElement != null) {
          // FIXME DO this a bit more proper
          // if (IceCandidate.class.equals(type)) {
          // return gson.fromJson(jsonElement, IceCandidate.class);
          // } else {
          return getAsJavaType(type, jsonElement);
          // }
        } else {
          // TODO Fail in this case
          if (type == boolean.class) {
            return false;
          } else if (type == int.class) {
            return 0;
          }
        }
      }

    } else {

      if (genericType instanceof ParameterizedType) {

        ParameterizedType genericMap = (ParameterizedType) genericType;

        if (Map.class.isAssignableFrom((Class<?>) genericMap.getRawType())
            && (genericMap.getActualTypeArguments()[0] == String.class)
            && (genericMap.getActualTypeArguments()[1] == String.class)) {

          Map<String, String> returnParams = new HashMap<String, String>();
          for (Entry<String, JsonElement> param : params.entrySet()) {
            String valueStr = !param.getValue().isJsonNull() ? param.getValue().getAsString()
                : null;
            returnParams.put(param.getKey(), valueStr);
          }

          return returnParams;
        }
      }
    }

    return null;
  }

  private Object getAsJavaType(Class<?> type, JsonElement jsonElement) {
    if (jsonElement.isJsonNull()) {
      return null;
    } else if (type == String.class) {
      return jsonElement.getAsString();
    } else if (type == boolean.class) {
      return jsonElement.getAsBoolean();
    } else if (type.isEnum()) {
      return gson.fromJson(jsonElement, type);
    } else if (type == int.class) {
      return jsonElement.getAsInt();
    } else {
      return gson.fromJson(jsonElement, type);
    }
  }
}
