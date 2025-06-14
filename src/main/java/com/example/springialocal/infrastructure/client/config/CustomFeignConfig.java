package com.example.springialocal.infrastructure.client.config;

import com.example.springialocal.domain.api.dto.ApiDtoResponse;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Configuration
public class CustomFeignConfig {

    @Bean
    public Decoder feignDecoder() {
        return new CustomDecoder();
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new CustomErrorDecoder();
    }

    static class CustomDecoder implements Decoder {
        @Override
        public Object decode(Response response, Type type) throws IOException {
            String body = "no_content";
            if (response.body() != null) {
                try (InputStream responseBodyStream = response.body().asInputStream()) {
                    body = StreamUtils.copyToString(responseBodyStream, StandardCharsets.UTF_8);
                }
            }

            if (response.status() >= 200 && response.status() < 300) {
                return new ApiDtoResponse(String.valueOf(response.status()), body);
            } else {
                return new ApiDtoResponse("error", "no_content");
            }
        }
    }

    static class CustomErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultErrorDecoder = new Default();
        @Override
        public Exception decode(String methodKey, Response response) {
            if (response.status() >= 400 && response.status() <= 599) {
                // For error statuses, we want the CustomDecoder to handle it.
                // By returning a non-Feign exception, we can make Feign fall back
                // to the decoder. This is a bit of a hack based on Feign's internals.
                return new Exception("Let CustomDecoder handle the error response.");
            }
            return defaultErrorDecoder.decode(methodKey, response);
        }
    }
} 