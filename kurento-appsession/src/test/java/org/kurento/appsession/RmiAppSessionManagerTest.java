package org.kurento.appsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.appsession.rmi.AppSessionId;
import org.kurento.appsession.rmi.AppSessionManagerJsonRpcHandler;
import org.kurento.appsession.rmi.JsonRpcMethod;
import org.kurento.appsession.rmi.JsonRpcNotification;
import org.kurento.appsession.rmi.RemoteParticipant;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcClientClosedException;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.Props;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.google.gson.JsonObject;

public class RmiAppSessionManagerTest {

  private static final Logger log = LoggerFactory.getLogger(RmiAppSessionManagerTest.class);

  public static interface TestRemoteParticipant extends RemoteParticipant {
    @JsonRpcNotification
    public void req();
  }

  public static class TestParticipant
      extends DefaultParticipant<TestAppSession, TestRemoteParticipant> {

  }

  public static class TestAppSession
      extends DefaultAppSession<TestParticipant, TestRemoteParticipant> {

    private int counter = 0;

    @JsonRpcMethod
    public String login(@AppSessionId String appSessionId, Session session) {
      return "OK";
    }

    @JsonRpcMethod
    public int incCounter() {
      counter++;
      return counter;
    }

    @JsonRpcMethod
    public int getCounter() {
      return counter;
    }

    @JsonRpcMethod
    public void reverse(TestRemoteParticipant remote) {
      log.info("Reverse request received");
      remote.req();
    }
  }

  public static class TestSessionManager
      extends AppSessionManager<TestAppSession, TestParticipant, TestRemoteParticipant> {

    @JsonRpcMethod
    public String nonAppSessionMethod(String param) {
      return param;
    }
  }

  @SpringBootApplication
  @Import(JsonRpcConfiguration.class)
  public static class App implements JsonRpcConfigurer {

    @Override
    public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
      registry.addHandler(appSessionHandler(), "/appsession");
    }

    @Bean
    public JsonRpcHandler<JsonObject> appSessionHandler() {
      return new AppSessionManagerJsonRpcHandler(sessionManager());
    }

    @Bean
    public TestSessionManager sessionManager() {
      return new TestSessionManager();
    }
  }

  @Test
  public void basicTest() throws InterruptedException, IOException {

    int port = 7777;

    ConfigurableApplicationContext app = SpringApplication.run(App.class,
        new String[] { "--server.port=" + port });

    JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://127.0.0.1:" + port + "/appsession");
    client.setSendCloseMessage(true);

    testWithClient(client);

    client.close();

    assertThatThrownBy(() -> client.sendRequest("exception"))
        .isInstanceOf(JsonRpcClientClosedException.class);

    // Time to session closing
    Thread.sleep(500);

    TestSessionManager sessionManager = (TestSessionManager) app.getBean("sessionManager");

    assertThat(sessionManager.getAppSessions()).hasSize(0);

    app.close();
  }

  public static void testWithClient(JsonRpcClientWebSocket client)
      throws IOException, InterruptedException {

    final CountDownLatch latch = new CountDownLatch(1);

    client.setServerRequestHandler(new DefaultJsonRpcHandler<String>() {
      @Override
      public void handleRequest(Transaction transaction, Request<String> request) throws Exception {
        if ("req".equals(request.getMethod())) {
          latch.countDown();
        }
      }
    });

    client.connect();

    String paramValue = "XX";
    String response = client.sendRequest("nonAppSessionMethod", new Props("param", paramValue),
        String.class);

    assertThat(response).isEqualTo(paramValue);

    response = client.sendRequest("login", new Props("appSessionId", "1"), String.class);

    assertThat(response).isEqualTo("OK");

    int initCounter = client.sendRequest("getCounter", Integer.class);

    int incCounter = client.sendRequest("incCounter", Integer.class);

    assertThat(incCounter).isEqualTo(initCounter + 1);

    client.sendRequest("reverse");

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

  }

}
