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

public class Client extends Observable implements PropertyChangeListener {
    /**
     * Client constructor.
     */
    public Client() {
        super();
    }

    /**
     * Start async client.
     * @param host
     * @param port
     * @throws IOException
     */
    public void start(final String host, final int port) throws IOException {
        // Create a socket channel.
        AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open();

        // Try to connect to the server side.
        socketChannel.connect(new InetSocketAddress(host, port), socketChannel, new CompletionHandler<Void, AsynchronousSocketChannel>() {
            @Override
            public void completed(Void result, AsynchronousSocketChannel attachment) {
                Client.this.notify("client:connected", attachment, null);
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                System.out.println("Failed to connect to server.");
                System.out.println("Retrying in 5 seconds...");

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

    private void write(AsynchronousSocketChannel socketChannel, String payload) {
        // Convert String to ByteBuffer.
        ByteBuffer byteBuffer = ByteBuffer.wrap(payload.getBytes());

        socketChannel.write(byteBuffer, socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel attachment) {

            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                System.out.println("Failed to write message to server.");
            }
        });
    }

    private void write(AsynchronousSocketChannel socketChannel, String type, JSONObject data) {
        JSONObject payload = new JSONObject();

        payload.put("type", type);
        payload.put("data", data);

        write(socketChannel, payload.toString());
    }

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
}
