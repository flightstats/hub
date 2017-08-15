package com.flightstats.hub.dao;

/**
 * Solution is based on work from http://www.flattermann.net/2009/01/read-a-zip-file-comment-with-java/
 */
class ZipComment {

    static String getZipCommentFromBuffer(byte[] buffer) {
        byte[] magicDirEnd = {0x50, 0x4b, 0x05, 0x06};
        int loopStart = buffer.length - magicDirEnd.length - 19;
        int loopEnd = loopStart - 8;
        for (int i = loopStart; i >= loopEnd; i--) {
            boolean isMagicStart = true;
            for (int k = 0; k < magicDirEnd.length; k++) {
                if (buffer[i + k] != magicDirEnd[k]) {
                    isMagicStart = false;
                    break;
                }
            }
            if (isMagicStart) {
                int commentLen = buffer[i + 20] + buffer[i + 21] * 256;
                int realLen = buffer.length - i - 22;
                return new String(buffer, i + 22, Math.min(commentLen, realLen));
            }
        }
        return null;
    }
}
