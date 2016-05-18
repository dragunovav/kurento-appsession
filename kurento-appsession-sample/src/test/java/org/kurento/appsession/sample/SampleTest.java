package org.kurento.appsession.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcClientClosedException;
import org.kurento.jsonrpc.Props;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class SampleTest {

  static final Logger log = LoggerFactory.getLogger(SampleTest.class);

  private int APP_PORT = 7777;

  private ConfigurableApplicationContext startApp() {
    ConfigurableApplicationContext app = SpringApplication.run(SampleApp.class,
        new String[] { "--server.port=" + APP_PORT });
    return app;
  }

  private JsonRpcClientWebSocket createClient(int port) {
    JsonRpcClientWebSocket client = new JsonRpcClientWebSocket(
        "ws://127.0.0.1:" + port + "/appsession");
    client.setSendCloseMessage(true);
    return client;
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

  @Test
  public void basicTest() throws InterruptedException, IOException {

    ConfigurableApplicationContext app = startApp();

    JsonRpcClientWebSocket client = createClient(APP_PORT);

    testWithClient(client);

    client.close();

    assertThatThrownBy(() -> client.sendRequest("exception"))
        .isInstanceOf(JsonRpcClientClosedException.class);

    // Time to session closing
    Thread.sleep(500);

    SampleAppSessionManager sessionManager = (SampleAppSessionManager) app
        .getBean("sessionManager");

    assertThat(sessionManager.getAppSessions()).hasSize(0);

    app.close();
  }

}
