package io.alchevrier.logstorageengine.hash;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MurmurHash3HashProvider implements HashProvider {

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;
    private static final int C3 = 0xe6546b64;
    private static final int C4 = 0x85ebca6b;
    private static final int C5 = 0xc2b2ae35;

    @Override
    public int hash(String key) {
        var toBeProcessed = key.getBytes();

        var bodyResult = 0;
        for (int i = 0; i < (toBeProcessed.length / 4) * 4; i = i + 4) {
            var chunk = toInt(Arrays.copyOfRange(toBeProcessed, i, i + 4));
            chunk = Integer.rotateLeft(chunk * C1, 15) * C2;
            bodyResult ^= chunk;
            bodyResult = Integer.rotateLeft(bodyResult, 13);
            bodyResult *= 5;
            bodyResult += C3;
        }

        var missingLastByte = toBeProcessed.length % 4;
        var tailResult = 0;
        switch (missingLastByte) {
            case 3: tailResult ^= (toBeProcessed[toBeProcessed.length - 3] & 0xff) << 16;
            case 2: tailResult ^= (toBeProcessed[toBeProcessed.length - 2] & 0xff) << 8;
            case 1: tailResult ^= toBeProcessed[toBeProcessed.length - 1] & 0xff ;
        }
        bodyResult ^= Integer.rotateLeft((tailResult * C1), 15) * C2;

        bodyResult ^= toBeProcessed.length;
        bodyResult ^= bodyResult >>> 16;
        bodyResult *= C4;
        bodyResult ^= bodyResult >>> 13;
        bodyResult *= C5;
        bodyResult ^= bodyResult >>> 16;

        return bodyResult;
    }

    private int toInt(byte[] chunk) {
        return ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
}
