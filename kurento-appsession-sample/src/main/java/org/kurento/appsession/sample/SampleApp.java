package org.kurento.appsession.sample;

import org.kurento.appsession.rmi.AppSessionManagerJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.google.gson.JsonObject;

@SpringBootApplication
@Import(JsonRpcConfiguration.class)
public class SampleApp implements JsonRpcConfigurer {

  @Override
  public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
    registry.addHandler(appSessionHandler(), "/appsession");
  }

  @Bean
  public JsonRpcHandler<JsonObject> appSessionHandler() {
    return new AppSessionManagerJsonRpcHandler(sessionManager());
  }

  @Bean
  public SampleAppSessionManager sessionManager() {
    return new SampleAppSessionManager();
  }
}