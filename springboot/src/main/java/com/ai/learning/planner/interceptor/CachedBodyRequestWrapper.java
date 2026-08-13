package com.ai.learning.planner.interceptor;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 可重读请求体包装器
 *
 * <p>与 ContentCachingRequestWrapper 的区别：后者只缓存"已读"内容，底层流一旦
 * 读完即为 EOF，后续 @RequestBody 再读会得到空 body（400 请求体缺失）；本包装器
 * 在构造时将 JSON 请求体整体读入内存，getInputStream()/getReader() 每次返回新的
 * 流，保证 SecurityInterceptor 与 Spring MVC 控制器都能完整读取请求体。
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        try (var in = request.getInputStream()) {
            this.body = in.readAllBytes();
        }
    }

    public byte[] getContentAsByteArray() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return byteStream.read();
            }

            @Override
            public int available() {
                return byteStream.available();
            }

            @Override
            public boolean isFinished() {
                return byteStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        String encoding = getCharacterEncoding();
        Charset charset = (encoding != null && Charset.isSupported(encoding))
                ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
}
