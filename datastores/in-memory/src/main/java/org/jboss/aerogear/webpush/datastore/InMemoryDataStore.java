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
package org.jboss.aerogear.webpush.datastore;


import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jboss.aerogear.webpush.Channel;
import org.jboss.aerogear.webpush.Registration;

/**
 * A {@link DataStore} implementation that stores all information in memory.
 */
public class InMemoryDataStore implements DataStore {

    private final ConcurrentMap<String, Registration> registrations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Channel>> channels = new ConcurrentHashMap<>();

    private byte[] salt;

    @Override
    public void savePrivateKeySalt(final byte[] salt) {
        if (this.salt != null) {
            this.salt = salt;
        }
    }

    @Override
    public byte[] getPrivateKeySalt() {
        if (salt == null) {
            return new byte[]{};
        }
        return salt;
    }

    @Override
    public boolean saveRegistration(final Registration registration) {
        Objects.requireNonNull(registration, "registration must not be null");
        return registrations.putIfAbsent(registration.id(), registration) == null;
    }

    @Override
    public Optional<Registration> getRegistration(final String id) {
        return Optional.ofNullable(registrations.get(id));
    }

    @Override
    public void saveChannel(final Channel channel) {
        final String id = channel.registrationId();
        final Set<Channel> newChannels = Collections.newSetFromMap(new ConcurrentHashMap<>());
        newChannels.add(channel);
        while (true) {
            final Set<Channel> currentChannels = channels.get(id);
            if (currentChannels == null) {
                final Set<Channel> previous = channels.putIfAbsent(id, newChannels);
                if (previous != null) {
                    newChannels.addAll(previous);
                    if (channels.replace(id, previous, newChannels)) {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                newChannels.addAll(currentChannels);
                if (channels.replace(id, currentChannels, newChannels)) {
                    break;
                }
            }
        }
    }

    @Override
    public void removeChannel(final Channel channel) {
        Objects.requireNonNull(channel, "channel must not be null");
        while (true) {
            final Set<Channel> currentChannels = channels.get(channel.registrationId());
            if (currentChannels == null || currentChannels.isEmpty()) {
                break;
            }
            final Set<Channel> newChannels = Collections.newSetFromMap(new ConcurrentHashMap<>());
            boolean added = newChannels.addAll(currentChannels);
            if (!added){
                break;
            }

            boolean removed = newChannels.remove(channel);
            if (removed) {
                if (channels.replace(channel.registrationId(), currentChannels, newChannels)) {
                    break;
                }
            }
        }
    }

    @Override
    public Set<Channel> getChannels(final String registrationId) {
        final Set<Channel> channels = this.channels.get(registrationId);
        if (channels == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(channels);
    }

}
