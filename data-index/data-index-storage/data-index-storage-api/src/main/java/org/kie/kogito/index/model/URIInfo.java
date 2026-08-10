package org.kie.kogito.index.model;

import java.net.URI;

public record URIInfo(URI truncatedURI, String idVersion) {
    public static URIInfo from(String endpoint, String id, String version) {
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        final String idVersion;
        if (version != null && endpoint.endsWith(id + '/' + version)) {
            idVersion = id + '/' + version;
        } else if (endpoint.endsWith(id)) {
            idVersion = id;
        } else {
            int lastIndex = endpoint.lastIndexOf("/");
            return new URIInfo(URI.create(lastIndex > 0 ? endpoint.substring(0, lastIndex) : endpoint), id);
        }
        endpoint = endpoint.substring(0, endpoint.length() - idVersion.length());
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return new URIInfo(URI.create(endpoint), idVersion);
    }

    public static URIInfo buildURIInfo(ProcessInstance instance) {
        return from(instance.getEndpoint(), instance.getProcessId(), instance.getVersion());
    }

    public static URIInfo buildURIInfo(ProcessDefinition definition) {
        return from(definition.getEndpoint(), definition.getId(), definition.getVersion());
    }

}
