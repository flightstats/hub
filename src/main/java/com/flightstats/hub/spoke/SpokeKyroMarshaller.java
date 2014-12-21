package com.flightstats.hub.spoke;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.flightstats.hub.model.Content2;
import com.flightstats.hub.model.ContentKey;
import com.ning.compress.lzf.LZFInputStream;
import com.ning.compress.lzf.LZFOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SpokeKyroMarshaller {

    private static final Kryo kryo = new Kryo();

    //todo - gfm - 12/20/14 - look at caching buffers
    //todo - gfm - 12/20/14 - look at streaming

    public static byte[] toBytes(Content2 content) throws IOException {
        /**
         * InputStream in = new LZFInputStream(new FileInputStream("data.lzf"));
         OutputStream out = new LZFOutputStream(new FileOutputStream("results.lzf"));
         InputStream compIn = new LZFCompressingInputStream(new FileInputStream("stuff.txt"));
         */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(new LZFOutputStream(baos));
        kryo.writeObject(output, content);
        output.close();
        return baos.toByteArray();
    }

    public static Content2 toContent(byte[] read, ContentKey key) throws IOException {
        Input input = new Input(new LZFInputStream(new ByteArrayInputStream(read)));
        Content2 content = kryo.readObject(input, Content2.class);
        input.close();
        content.setContentKey(key);
        return content;
    }

}
