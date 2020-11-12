package com.dist.system.info.client;

import com.dist.system.info.util.Observable;
import org.json.JSONObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class Client extends Observable implements PropertyChangeListener, Runnable {
    String host;
    int port;

    /**
     * Client constructor.
     * @param host
     * @param port
     */
    public Client(String host, int port) {
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
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open();

        // Try to connect to the server side.
        socketChannel.connect(new InetSocketAddress(host, port), socketChannel, new CompletionHandler<Void, AsynchronousSocketChannel>() {
            @Override
            public void completed(Void result, AsynchronousSocketChannel attachment) {
                System.out.println("[Client] Connected to server.");

                Client.this.notify("client:connected", attachment, null);
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                System.out.println("[Client] Failed to connect to server.");
                System.out.println("[Client] Retrying in 5 seconds...");

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    start(host, port);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        });
    }

    /**
     * Write to server.
     * @param socketChannel
     * @param payload
     */
    private void write(AsynchronousSocketChannel socketChannel, String payload) {
        // Convert String to ByteBuffer.
        ByteBuffer byteBuffer = ByteBuffer.wrap(payload.getBytes());

        socketChannel.write(byteBuffer, socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel attachment) {

            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                System.out.println("[Client] Failed to write message to server.");
            }
        });
    }

    /**
     * Write JSONObject to server.
     * @param socketChannel
     * @param type
     * @param data
     */
    private void write(AsynchronousSocketChannel socketChannel, String type, JSONObject data) {
        JSONObject payload = new JSONObject();

        payload.put("type", type);
        payload.put("data", data);

        write(socketChannel, payload.toString());
    }

    /**
     * PropertyChangeListener propertyChange.
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String event = evt.getPropertyName();

        switch (event) {
            case "system:info:ready": {
                AsynchronousSocketChannel socketChannel = (AsynchronousSocketChannel) evt.getOldValue();
                JSONObject data = (JSONObject) evt.getNewValue();

                write(socketChannel, "system:info", data);
                break;
            }
            default: break;
        }
    }

    /**
     * Runnable run.
     */
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
