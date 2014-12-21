package com.flightstats.hub.spoke;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SpokeKyroMarshaller {

    private static final Kryo kryo = new Kryo();
    private static final ContentSerializer serializer = new ContentSerializer();

    //todo - gfm - 12/20/14 - look at caching buffers
    //todo - gfm - 12/20/14 - look at streaming
    //todo - gfm - 12/21/14 - look at compression

    public static byte[] toBytes(Content content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //Output output = new Output(new LZFOutputStream(baos));
        Output output = new Output(baos);
        kryo.writeObject(output, content, serializer);
        output.close();
        return baos.toByteArray();
    }

    public static Content toContent(byte[] read, ContentKey key) throws IOException {
        Input input = new Input(new ByteArrayInputStream(read));
        //Input input = new Input(new LZFInputStream(new ByteArrayInputStream(read)));
        Content content = kryo.readObject(input, Content.class, serializer);
        input.close();
        content.setContentKey(key);
        return content;
    }

    static class ContentSerializer extends Serializer<Content> {

        @Override
        public void write(Kryo kryo, Output output, Content content) {
            output.writeString(content.getUser().orNull());
            output.writeString(content.getContentLanguage().orNull());
            output.writeString(content.getContentType().orNull());
            output.writeInt(content.getData().length);
            output.write(content.getData());
        }

        @Override
        public Content read(Kryo kryo, Input input, Class<Content> type) {
            Content.Builder builder = Content.builder()
                    .withUser(input.readString())
                    .withContentLanguage(input.readString())
                    .withContentType(input.readString());
            int dataLength = input.readInt();
            return builder.withData(input.readBytes(dataLength)).build();
        }
    }

}
