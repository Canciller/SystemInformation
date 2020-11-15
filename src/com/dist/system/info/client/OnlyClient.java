package com.dist.system.info.client;

import com.dist.system.info.util.Observer;
import com.dist.system.info.util.Payload;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.*;

public class OnlyClient extends Observer implements Runnable {
    String host;
    int port;

    AsynchronousSocketChannel socketChannel;

    ByteBuffer writeBuffer = ByteBuffer.allocate(4096);
    Future<Integer> writeFuture = null;

    ByteBuffer readBuffer = ByteBuffer.allocate(4096);

    /**
     * Client constructor.
     * @param host
     * @param port
     */
    public OnlyClient(String host, int port) {
        super();

        this.host = host;
        this.port = port;
    }

    /**
     * Start async client with new host and port.
     * @param host
     * @param port
     * @throws IOException
     */
    private void start(String host, int port) throws IOException {
        this.host = host;
        this.port = port;

        start();
    }

    /**
     * Start async client.
     * @throws IOException
     */
    private void start() throws IOException {
        // Create a socket channel.
        socketChannel = AsynchronousSocketChannel.open();

        try {
            socketChannel.connect(new InetSocketAddress(host, port)).get();
            System.out.println("[Client] Connected to server.");

            /*
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000);
                        socketChannel.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            thread.start();
             */

            read();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write to server.
     * @param payload
     */
    private void write(final String payload) {
        while (writeFuture != null) {
            try {
                int bytesWrite = writeFuture.get();
                if(bytesWrite == -1) throw new Exception("Failed to write buffer.");
            } catch (Exception e) {
                System.out.println("[Client] Write failed.");
                //e.printStackTrace();
            }

            break;
        }

        writeBuffer = ByteBuffer.wrap(payload.getBytes());
        writeFuture = socketChannel.write(writeBuffer);
    }

    /**
     * Write to server.
     * @param payload
     */
    public void write(Payload payload) {
        write(payload.toString());
    }

    /**
     * Read from server
     */
    private void read() {
        readBuffer.clear();
        socketChannel.read(readBuffer, socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer bytesRead, AsynchronousSocketChannel socketChannel) {
                if(bytesRead != -1 ) {
                    Payload payload = new Payload(readBuffer, bytesRead);
                    System.out.println("[Client] Read: " + payload);

                    read();
                } else {
                    failed(new Exception("Failed to read buffer."), socketChannel);
                }
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel socketChannel) {
                System.out.println("[Client] Read failed.");
            }
        });
    }

    /**
     * Close channel.
     */
    private void close() {
        if(!isOpen()) return;

        try {
            socketChannel.close();
            System.out.println("[Client] Server connection closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Check if channel is open.
     * @return
     */
    private boolean isOpen() {
        return socketChannel != null && socketChannel.isOpen();
    }

    @Override
    public void update(String eventType, Object oldValue, Object newValue) {
        //System.out.println("[Client] Client event: " + eventType);

        Payload payload = new Payload();

        switch (eventType) {
            case "system:info:get:done": {
                payload.setHeaderType("client:system:info");
                payload.setBody((JSONObject) newValue);
                write(payload);
                break;
            }
        }
    }

    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            // TODO: Handle error.
            e.printStackTrace();
        }
    }
}
