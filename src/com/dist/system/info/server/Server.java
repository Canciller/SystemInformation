package com.dist.system.info.server;

import com.dist.system.info.util.Observable;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends Observable {
    ConcurrentHashMap<String, AsynchronousSocketChannel> clients;

    /**
     * Server constructor.
     */
    public Server() {
        super();

        clients = new ConcurrentHashMap<>();
    }

    /**
     * Start async server.
     * @param host
     * @param port
     * @throws IOException
     */
    public void start(String host, int port) throws IOException {
        InetSocketAddress socketAddress = new InetSocketAddress(host, port);

        // Create a socket channel and bind to local bind address.
        AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open().bind(socketAddress);

        System.out.format("Servidor abierto en %s:%d\n", host, port);

        // Start to accept connections from clients.
        serverSocketChannel.accept(serverSocketChannel, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {
            @Override
            public void completed(AsynchronousSocketChannel result, AsynchronousServerSocketChannel attachment) {
                try {
                    InetSocketAddress socketAddress = (InetSocketAddress) result.getRemoteAddress();
                    System.out.format("Conexión aceptada %s\n", socketAddress.getHostName());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                attachment.accept(attachment, this);

                read(result);
            }

            @Override
            public void failed(Throwable exc, AsynchronousServerSocketChannel attachment) {
                // TODO: Handle accept failed.
            }
        });
    }

    /**
     * Read buffer received from client.
     * @param socketChannel
     */
    private void read(final AsynchronousSocketChannel socketChannel) {
        final ByteBuffer buffer = ByteBuffer.allocate(2048);

        // Read message from client.
        socketChannel.read(buffer, socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer result, AsynchronousSocketChannel attachment) {
                try {
                    InetSocketAddress socketAddress = (InetSocketAddress) attachment.getRemoteAddress();
                    addClient(socketAddress.getHostName(), attachment);

                    String payload = new String(buffer.array(), StandardCharsets.UTF_8);

                    // TODO: Handle parse error.
                    JSONObject object = new JSONObject(payload);
                    object.put("hostname", socketAddress.getHostName());

                    Server.this.notify(object.getString("type"), null, object);
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
                notifyClientDisconnect(attachment);
            }
        });
    }

    /**
     * Write buffer to client.
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
                notifyClientDisconnect(attachment);
            }
        });
    }

    /**
     * Write JSONObject to client.
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
     * Broadcast JSONObject to all clients.
     * @param type
     * @param data
     */
    private void broadcast(String type, JSONObject data) {
        for (String hostname : clients.keySet()) {
            AsynchronousSocketChannel socketChannel = clients.get(hostname);
            write(socketChannel, type, data);
        }
    }

    /**
     * Notify client disconnected.
     * @param socketChannel
     */
    private void notifyClientDisconnect(AsynchronousSocketChannel socketChannel) {
        try {
            InetSocketAddress socketAddress = (InetSocketAddress) socketChannel.getRemoteAddress();

            // Remove client from list.
            removeClient(socketAddress.getHostName());

            String type = "client:disconnected";

            JSONObject object = new JSONObject();
            object.put("hostname", socketAddress.getHostName());
            object.put("type", type);

            Server.this.notify(type, null, object);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addClient(String hostname, AsynchronousSocketChannel socketChannel) {
        clients.put(hostname, socketChannel);
    }

    private void removeClient(String hostname) {
        clients.remove(hostname);
    }
}
