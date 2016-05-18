/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.appsession.basicroom;

import org.kurento.appsession.rmi.AppSessionManagerJsonRpcHandler;
import org.kurento.client.KurentoClient;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.google.gson.JsonObject;

@SpringBootApplication
@Import(JsonRpcConfiguration.class)
public class RoomApp implements JsonRpcConfigurer {

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create();
  }

  @Override
  public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
    registry.addHandler(appSessionHandler(), "/room");
  }

  @Bean
  public JsonRpcHandler<JsonObject> appSessionHandler() {
    return new AppSessionManagerJsonRpcHandler(roomManager());
  }

  @Bean
  public RoomManager roomManager() {
    return new RoomManager();
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(RoomApp.class, args);
  }
}
