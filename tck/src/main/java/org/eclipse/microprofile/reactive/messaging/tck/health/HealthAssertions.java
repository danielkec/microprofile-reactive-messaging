/**
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.microprofile.reactive.messaging.tck.health;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import static org.junit.Assert.assertEquals;

class HealthAssertions {

    private int responseCode;
    private JsonObject jsonResponse;

    static HealthAssertions create(String url) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            con.connect();
            HealthAssertions healthAssertions = new HealthAssertions();
            healthAssertions.responseCode = con.getResponseCode();

            InputStream is = con.getErrorStream();
            if(is == null){
                is = con.getInputStream();
            }
            JsonReader jsonReader = Json.createReader(is);
            healthAssertions.jsonResponse = jsonReader.readObject();
            is.close();
            con.disconnect();
            return healthAssertions;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    HealthAssertions assertResponseCode(int expected){
        assertEquals(expected, this.responseCode);
        return this;
    }

    HealthAssertions assertResponseCodeUp(){
        assertResponseCode(200);
        return this;
    }

    HealthAssertions assertResponseCodeDown(){
        assertResponseCode(503);
        return this;
    }

    HealthAssertions assertMessagingStatus(String expectedStatus){
        JsonArray checks = this.jsonResponse.getJsonArray("checks");
        List<JsonObject> messagingObjects = checks.stream()
            .map(JsonValue::asJsonObject)
            .filter(o -> o.containsKey("name") && "messaging".equals(o.getString("name")))
            .collect(Collectors.toList());
        assertEquals(1, messagingObjects.size());
        JsonObject messagingObject = messagingObjects.get(0);
        assertEquals(expectedStatus, messagingObject.getString("status"));
        return this;
    }

    HealthAssertions assertMessagingStatusUp(){
        assertMessagingStatus("UP");
        return this;
    }

    HealthAssertions assertMessagingStatusDown(){
        assertMessagingStatus("DOWN");
        return this;
    }
}
