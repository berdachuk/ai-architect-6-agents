package com.berdachuk.meteoris.insight.core;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * MongoDB-style 24-character lowercase hex ids (ObjectId byte layout: 4-byte unix seconds,
 * 5 random bytes, 3-byte counter seeded random).
 */
@Component
public final class IdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final AtomicInteger COUNTER = new AtomicInteger(new SecureRandom().nextInt());

    public String generateId() {
        int time = (int) (Instant.now().getEpochSecond());
        byte[] random = new byte[5];
        SECURE_RANDOM.nextBytes(random);
        int c = COUNTER.getAndIncrement() & 0xffffff;
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.putInt(time);
        buf.put(random);
        buf.put((byte) ((c >> 16) & 0xff));
        buf.put((byte) ((c >> 8) & 0xff));
        buf.put((byte) (c & 0xff));
        return HexFormat.of().formatHex(buf.array());
    }

    public boolean isValidId(String id) {
        if (id == null || id.length() != 24) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char ch = id.charAt(i);
            if ((ch < '0' || ch > '9') && (ch < 'a' || ch > 'f')) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts the creation instant encoded in the first four bytes (unix seconds).
     */
    public Instant extractCreationInstant(String id) {
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Invalid id: " + id);
        }
        byte[] bytes = HexFormat.of().parseHex(id);
        int epochSeconds = ByteBuffer.wrap(bytes).getInt(0);
        return Instant.ofEpochSecond(Integer.toUnsignedLong(epochSeconds));
    }
}
