package edu.upc.dmag.signinginterface;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

public class AsicSHttpMessageConverter extends AbstractHttpMessageConverter<StreamingResponseBody> {

    public AsicSHttpMessageConverter() {
        super(MediaType.valueOf("application/vnd.etsi.asic-s+zip"));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return StreamingResponseBody.class.isAssignableFrom(clazz);
    }

    @Override
    protected StreamingResponseBody readInternal(Class<? extends StreamingResponseBody> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("This converter can only be used for writing");
    }

    @Override
    protected void writeInternal(StreamingResponseBody streamingResponseBody, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        streamingResponseBody.writeTo(outputMessage.getBody());
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }
}

