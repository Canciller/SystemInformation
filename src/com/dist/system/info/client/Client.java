package com.dist.system.info.client;

import com.dist.system.info.util.Observable;
import org.json.JSONObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;

public class Client extends Observable implements PropertyChangeListener, Runnable {
    String host;
    int port;

    AsynchronousSocketChannel socketChannel;

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
        socketChannel = AsynchronousSocketChannel.open();

        // Try to connect to the server side.
        socketChannel.connect(new InetSocketAddress(host, port), socketChannel, new CompletionHandler<Void, AsynchronousSocketChannel>() {
            @Override
            public void completed(Void result, AsynchronousSocketChannel attachment) {
                System.out.println("[Client] Connected to server.");

                Client.this.notifyObservers("client:connected", attachment, null);
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
                read(attachment);
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

    private void read(final AsynchronousSocketChannel socketChannel) {
        final ByteBuffer buffer = ByteBuffer.allocate(2048);

        // Read message from client.
        socketChannel.read(buffer, socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel attachment) {
                try {
                    InetSocketAddress socketAddress = (InetSocketAddress) attachment.getRemoteAddress();
                    String hostname = socketAddress.getHostName().toUpperCase();

                    String payload = new String(buffer.array(), StandardCharsets.UTF_8);

                    // TODO: Handle parse error.
                    JSONObject object = new JSONObject(payload);
                    System.out.println(object.toString(2));

                    String type = object.getString("type");

                    // Handle server switch.
                    if(type.equals("server:switch"))
                        handleServerSwitch(object.getJSONObject("data"));
                    else
                        Client.this.notifyObservers(type, null, object);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // Start to read next message again.
                    if(attachment.isOpen())
                        read(attachment);
                }
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                // TODO: Handle read error.
            }
        });
    }

    /**
     * Handle server switch.
     */
    void handleServerSwitch(JSONObject data) throws IOException {
        String host = data.getString("ip_address");
        int port = data.getInt("port");

        // TODO: Handle close error.
        //socketChannel.shutdownInput();
        //socketChannel.shutdownOutput();
        socketChannel.close();

        start(host, port);
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
