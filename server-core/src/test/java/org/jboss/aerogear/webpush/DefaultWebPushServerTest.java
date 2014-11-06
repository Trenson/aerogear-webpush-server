/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.webpush;

import org.jboss.aerogear.webpush.datastore.DataStore;
import org.jboss.aerogear.webpush.datastore.InMemoryDataStore;
import org.jboss.aerogear.webpush.datastore.RegistrationNotFoundException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class DefaultWebPushServerTest {

    private DefaultWebPushServer server;

    @Before
    public void setup() {
        final DataStore dataStore = new InMemoryDataStore();
        final WebPushServerConfig config = DefaultWebPushConfig.create().password("test").build();
        final byte[] privateKey = DefaultWebPushServer.generateAndStorePrivateKey(dataStore, config);
        server = new DefaultWebPushServer(dataStore, config, privateKey);
    }

    @Test
    public void register() {
        final Registration reg = server.register();
        assertThat(reg.id(), is(notNullValue()));
        assertThat(reg.monitorURI().toString(), equalTo("webpush:" + reg.id() + ":push:monitor"));
        assertThat(reg.channelURI().toString(), equalTo("webpush:" + reg.id() + ":push:channel"));
    }

    @Test
    public void newChannel() throws RegistrationNotFoundException {
        final Registration reg = server.register();
        final Channel ch = server.newChannel(reg.id());
        assertThat(ch.registrationId(), equalTo(reg.id()));
        assertThat(ch.message(), equalTo(DefaultChannel.NONE));
    }

    @Test
    public void setAndGetMessage() throws RegistrationNotFoundException {
        final Registration reg = server.register();
        final Channel ch = server.newChannel(reg.id());
        server.setMessage(ch.endpointToken(), "some message");
        final String message = server.getMessage(ch.endpointToken());
        assertThat(message, equalTo("some message"));
    }

}
