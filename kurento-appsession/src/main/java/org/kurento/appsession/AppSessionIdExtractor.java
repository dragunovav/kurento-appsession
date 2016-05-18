package org.kurento.appsession;

import org.kurento.jsonrpc.message.Request;

import com.google.gson.JsonObject;

public interface AppSessionIdExtractor {

  String extractAppSessionIdFromRequest(Request<JsonObject> request);

}
