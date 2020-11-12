package com.dist.system.info.server;

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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class Server extends Observable implements PropertyChangeListener, Runnable {
    String host;
    int port;

    ConcurrentHashMap<String, AsynchronousSocketChannel> clients;

    /**
     * Server constructor.
     * @param host
     * @param port
     */
    public Server(String host, int port) {
        super();

        this.host = host;
        this.port = port;

        clients = new ConcurrentHashMap<>();
    }

    /**
     * Start async server with new host and port.
     * @param host
     * @param port
     * @throws IOException
     */
    private void start(String host, int port) throws IOException {
        this.host = host;
        this.port = port;

        start();
    }

    private void start() throws IOException {
        InetSocketAddress socketAddress = new InetSocketAddress(host, port);

        // Create a socket channel and bind to local bind address.
        AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open().bind(socketAddress);

        System.out.format("[Server] Server binded %s:%d\n", host, port);

        // Start to accept connections from clients.
        serverSocketChannel.accept(serverSocketChannel, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {
            @Override
            public void completed(AsynchronousSocketChannel result, AsynchronousServerSocketChannel attachment) {
                try {
                    InetSocketAddress socketAddress = (InetSocketAddress) result.getRemoteAddress();
                    System.out.format("[Server] Connection accepted %s\n", socketAddress.getHostName());
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
                    String hostname = socketAddress.getHostName().toUpperCase();
                    addClient(hostname, attachment);

                    String payload = new String(buffer.array(), StandardCharsets.UTF_8);


                    // TODO: Handle parse error.
                    JSONObject object = new JSONObject(payload);

                    object.put("connected", true);
                    object.put("ip_address", socketAddress.getAddress().getHostAddress());
                    object.put("hostname", hostname);

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
     * Write to client.
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
            String hostname = socketAddress.getHostName().toUpperCase();

            // Remove client from list.
            removeClient(hostname);

            String type = "client:disconnected";

            JSONObject object = new JSONObject();
            object.put("connected", false);
            object.put("hostname", hostname);
            object.put("ip_address", socketAddress.getAddress().getHostAddress());
            object.put("type", type);

            Server.this.notify(type, null, object);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add client to clients.
     * @param hostname
     * @param socketChannel
     */
    private void addClient(String hostname, AsynchronousSocketChannel socketChannel) {
        clients.put(hostname, socketChannel);
    }

    /**
     * Remove client from clients.
     * @param hostname
     */
    private void removeClient(String hostname) {
        clients.remove(hostname);
    }

    /**
     * Get client from clients.
     * @param hostname
     */
    private AsynchronousSocketChannel getClient(String hostname) {
        return clients.get(hostname);
    }

    /**
     * PropertyChangeListener propertyChange.
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String event = evt.getPropertyName();
        switch (event) {
            case "ranking:new:max": {
                String hostname = (String) evt.getOldValue();
                Long rank = (Long) evt.getNewValue();

                System.out.format("[Server] Client with new max rank %s %d\n", hostname, rank);

                AsynchronousSocketChannel socketChannel = getClient(hostname);

                if(!socketChannel.isOpen()) break;

                InetSocketAddress socketAddress = null;

                try {
                    socketAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                // Ignore same hostname.
                if(hostname.equals(socketAddress.getHostName().toUpperCase())) break;

                // TODO: Implement server and client switching.
                JSONObject data = new JSONObject();

                data.put("ip_address", socketAddress.getAddress().getHostAddress());
                // TODO: Get port from server socket.
                data.put("port", 25565);

                broadcast("server:switch", data);

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
