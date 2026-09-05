package com.filestorage.virusscan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Component
class ClamAvClient {
    private final String host;
    private final int port;
    private final int chunkSize;

    ClamAvClient(@Value("${app.clamav.host:localhost}") String host,
                 @Value("${app.clamav.port:3310}") int port,
                 @Value("${app.clamav.chunk-size:8192}") int chunkSize) {
        this.host = host;
        this.port = port;
        this.chunkSize = chunkSize;
    }

    ScanVerdict scan(InputStream input) throws Exception {
        try (Socket socket = new Socket(host, port)) {
            OutputStream output = socket.getOutputStream();
            output.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            byte[] buffer = new byte[chunkSize];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(ByteBuffer.allocate(4).putInt(read).array());
                output.write(buffer, 0, read);
            }
            output.write(new byte[]{0, 0, 0, 0});
            output.flush();

            ByteArrayOutputStream response = new ByteArrayOutputStream();
            socket.getInputStream().transferTo(response);
            String text = response.toString(StandardCharsets.UTF_8);
            if (text.contains("FOUND")) {
                return new ScanVerdict(false, text.trim());
            }
            return new ScanVerdict(true, text.trim());
        }
    }
}

record ScanVerdict(boolean clean, String signature) {
}
