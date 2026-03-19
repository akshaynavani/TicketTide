package com.tickettide.event.storage;

import java.io.InputStream;
import java.util.Optional;

public interface ImageStorage {

    /** Stores the object and returns its key. */
    String put(String key, InputStream content, long length, String contentType);

    void delete(String key);

    /** Short-lived URL a browser can use to fetch the object directly. */
    Optional<String> publicUrl(String key);
}
