package com.luanlana.chat_server.log;

import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LogWriter {

    private final BufferedWriter writer;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    public LogWriter(String fileName) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(fileName, true));
    }

    public synchronized void print(String message) {
        try {
            writer.write("[");
            String timestamp = formatter.format(Instant.now());
            writer.write(timestamp);
            writer.write("]");
            writer.write(" - " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("ERRO AO ESCREVER NO LOG" + e.getMessage());
        }
    }

}
