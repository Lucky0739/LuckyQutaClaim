package com.lucky.seckill.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 最终版：对 application/json 请求体做**字节级** BOM 清理。
 * 直接移除所有 0xEF 0xBB 0xBF 序列，无论出现在开头还是中间。
 * 这是 Apifox / 某些工具把 BOM 插入到 JSON 中间导致解析失败的唯一可靠解法。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class StripJsonUtf8BomFilter extends OncePerRequestFilter {

    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String contentType = request.getContentType();
        if (!isJsonContentType(contentType)) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] raw = readBody(request.getInputStream(), MAX_JSON_BYTES, response);
        if (raw == null) {
            return;
        }

        byte[] cleaned = removeAllUtf8Bom(raw);

        // 调试日志：只要原始长度和清理后长度不同，就打印
        if (raw.length != cleaned.length) {
            log.warn("【BOM 清理生效】原始 body 长度={}, 清理后长度={}, 已移除 {} 个 BOM 序列",
                    raw.length, cleaned.length, (raw.length - cleaned.length) / 3);
        }

        filterChain.doFilter(new JsonBodyRequestWrapper(request, cleaned), response);
    }

    private static boolean isJsonContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        return contentType.toLowerCase().contains("json");
    }

    private static byte[] readBody(ServletInputStream in, int max, HttpServletResponse resp) throws IOException {
        byte[] buf = in.readNBytes(max + 1);
        if (buf.length > max) {
            resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return null;
        }
        return buf;
    }

    /**
     * 彻底移除 byte[] 中所有连续的 EF BB BF 序列。
     * 这是最可靠的方式，不会依赖字符编码转换。
     */
    private static byte[] removeAllUtf8Bom(byte[] input) {
        if (input == null || input.length == 0) {
            return input;
        }

        int bomCount = 0;
        for (int i = 0; i < input.length - 2; i++) {
            if (input[i] == (byte) 0xEF && input[i + 1] == (byte) 0xBB && input[i + 2] == (byte) 0xBF) {
                bomCount++;
            }
        }

        if (bomCount == 0) {
            return input; // 没有 BOM，直接返回原数组（零拷贝）
        }

        byte[] output = new byte[input.length - bomCount * 3];
        int writeIndex = 0;
        for (int i = 0; i < input.length; ) {
            if (i + 2 < input.length
                    && input[i] == (byte) 0xEF
                    && input[i + 1] == (byte) 0xBB
                    && input[i + 2] == (byte) 0xBF) {
                i += 3; // 跳过 BOM
            } else {
                output[writeIndex++] = input[i++];
            }
        }
        return output;
    }

    private static final class JsonBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        JsonBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public String getContentType() {
            return MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";
        }

        @Override
        public String getCharacterEncoding() {
            return StandardCharsets.UTF_8.name();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream delegate = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return delegate.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return delegate.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return delegate.available() == 0;
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
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
