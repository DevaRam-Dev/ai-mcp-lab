package com.mcplab.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/*
 * WHY THIS CLASS EXISTS
 * =====================
 * StdioServerTransportProvider sends and receives JSON-RPC messages over
 * stdin/stdout with no built-in visibility.  When you call a tool in
 * MCP Inspector you see the result but not the protocol envelope (jsonrpc,
 * id, method, params).  This decorator intercepts every byte that crosses
 * the stdin/stdout boundary and logs the complete raw messages to STDERR
 * so you can study the actual wire format.
 *
 * HOW IT WORKS
 * ============
 * The SDK's 3-arg constructor accepts custom InputStream and OutputStream
 * implementations.  This class passes two logging wrappers:
 *
 *   LoggingInputStream  — buffers bytes from System.in until '\n', then
 *                         logs the complete JSON line to stderr BEFORE
 *                         passing it on to the SDK for processing.
 *
 *   LoggingOutputStream — passes each byte to System.out immediately,
 *                         while also buffering until '\n', then logs the
 *                         complete JSON line to stderr AFTER writing it.
 *
 * STDOUT IS SACRED — READ THIS
 * ============================
 * The MCP stdio transport uses stdout as its exclusive data channel.
 * Any non-JSON-RPC byte written to stdout will corrupt the framing and
 * the AI client (Inspector / Claude Desktop) will disconnect with a
 * JSON parse error.
 *
 * Wire messages are therefore logged with System.err.println() directly,
 * NOT through the SLF4J console appender.  The console appender in Spring
 * Boot writes to System.out by default — routing through it would break
 * the protocol.  System.err is guaranteed to reach Inspector's
 * "Error output" panel without touching the transport channel.
 *
 * TO DISABLE THIS LOGGING
 * =======================
 * In McpServerConfig.stdioTransportProvider(), change:
 *   return new LoggingStdioTransportProvider();
 * back to:
 *   return new StdioServerTransportProvider();
 * No other code changes are needed.
 */
@Slf4j
public class LoggingStdioTransportProvider extends StdioServerTransportProvider {

    public LoggingStdioTransportProvider() {
        super(new ObjectMapper(), new LoggingInputStream(System.in), new LoggingOutputStream(System.out));
        System.err.println("=========================================");
        System.err.println("MCP wire logging active — inbound/outbound JSON-RPC lines appear on stderr");
        System.err.println("=========================================");
        log.info("LoggingStdioTransportProvider constructed — wire logging banner emitted to stderr");
    }

    // =========================================================================
    // INBOUND INTERCEPTOR
    // Reads bytes from System.in, passes them through to the SDK unchanged,
    // and logs each complete JSON line (terminated by '\n') to stderr.
    // =========================================================================
    private static final class LoggingInputStream extends InputStream {

        private final InputStream wrapped;
        private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(4096);

        LoggingInputStream(InputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int read() throws IOException {
            int b = wrapped.read();
            if (b != -1) {
                if (b == '\n') {
                    emitInbound(lineBuffer.toString(StandardCharsets.UTF_8));
                    lineBuffer.reset();
                } else {
                    lineBuffer.write(b);
                }
            }
            return b;
        }

        // The SDK reads in chunks — override the bulk-read method or only
        // single-byte reads would be intercepted.
        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int n = wrapped.read(buf, off, len);
            if (n > 0) {
                for (int i = 0; i < n; i++) {
                    byte b = buf[off + i];
                    if (b == '\n') {
                        emitInbound(lineBuffer.toString(StandardCharsets.UTF_8));
                        lineBuffer.reset();
                    } else {
                        lineBuffer.write(b);
                    }
                }
            }
            return n;
        }

        @Override
        public int available() throws IOException {
            return wrapped.available();
        }

        private static void emitInbound(String line) {
            if (!line.isEmpty()) {
                // Write directly to stderr — see class-level comment on why
                // we bypass the SLF4J console appender here.
                System.err.println(">>> MCP IN  : " + line);
                System.err.flush();
            }
        }
    }

    // =========================================================================
    // OUTBOUND INTERCEPTOR
    // The SDK writes to this stream; bytes are forwarded to real System.out
    // immediately (preserving protocol framing), and each completed JSON line
    // is also logged to stderr.
    // =========================================================================
    private static final class LoggingOutputStream extends OutputStream {

        private final OutputStream wrapped;
        private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(4096);

        LoggingOutputStream(OutputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void write(int b) throws IOException {
            // Forward to real stdout FIRST — logging is a side effect, not a gate.
            wrapped.write(b);
            if (b == '\n') {
                emitOutbound(lineBuffer.toString(StandardCharsets.UTF_8));
                lineBuffer.reset();
            } else {
                lineBuffer.write(b);
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            wrapped.write(buf, off, len);
            for (int i = 0; i < len; i++) {
                byte b = buf[off + i];
                if (b == '\n') {
                    emitOutbound(lineBuffer.toString(StandardCharsets.UTF_8));
                    lineBuffer.reset();
                } else {
                    lineBuffer.write(b);
                }
            }
        }

        @Override
        public void flush() throws IOException {
            wrapped.flush();
        }

        private static void emitOutbound(String line) {
            if (!line.isEmpty()) {
                System.err.println("<<< MCP OUT : " + line);
                System.err.flush();
            }
        }
    }
}
