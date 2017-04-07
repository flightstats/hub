package com.flightstats.hub.dao;

public class ZipComment {

    /*public static String extractZipComment(String filename) {
        String retStr = null;
        try {
            File file = new File(filename);
            int fileLen = (int) file.length();
            FileInputStream in = new FileInputStream(file);
            *//* The whole ZIP comment (including the magic byte sequence)
            * MUST fit in the buffer
            * otherwise, the comment will not be recognized correctly
            *
            * You can safely increase the buffer size if you like
            *//*
            //todo - gfm - long is 64 bytes
            byte[] buffer = new byte[Math.min(fileLen, 8192)];
            int bytesRead;
            in.skip(fileLen - buffer.length);
            if ((bytesRead = in.read(buffer)) > 0) {
                retStr = getZipCommentFromBuffer(buffer, bytesRead);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retStr;
    }
    //todo - gfm - remove this, after reducing length sent to getZipCommentFromBuffer
    */

    static String getZipCommentFromBuffer(byte[] buffer, int len) {
        byte[] magicDirEnd = {0x50, 0x4b, 0x05, 0x06};
        int buffLen = Math.min(buffer.length, len);
        for (int i = buffLen - magicDirEnd.length - 22; i >= 0; i--) {
            boolean isMagicStart = true;
            for (int k = 0; k < magicDirEnd.length; k++) {
                if (buffer[i + k] != magicDirEnd[k]) {
                    isMagicStart = false;
                    break;
                }
            }
            if (isMagicStart) {
                int commentLen = buffer[i + 20] + buffer[i + 21] * 256;
                int realLen = buffLen - i - 22;
                return new String(buffer, i + 22, Math.min(commentLen, realLen));
            }
        }
        return null;
    }
}
