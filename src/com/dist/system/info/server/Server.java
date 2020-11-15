package com.dist.system.info.server;

import com.dist.system.info.util.Observer;
import com.dist.system.info.util.Payload;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.*;

public class Server extends Observer implements Runnable {
    String host;
    int port;

    ConcurrentHashMap<String, AsynchronousSocketChannel> channels;
    ConcurrentHashMap<String, Future<Integer>> writeFutures;

    AsynchronousServerSocketChannel serverSocketChannel;

    /**
     * Server constructor.
     * @param host
     * @param port
     */
    public Server(String host, int port) {
        super();

        this.host = host;
        this.port = port;

        channels = new ConcurrentHashMap<>();
        writeFutures = new ConcurrentHashMap<>();
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
        final InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        serverSocketChannel = AsynchronousServerSocketChannel.open().bind(socketAddress);

        System.out.format("[Server] Server binded %s:%d\n", host, port);

        serverSocketChannel.accept(serverSocketChannel, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {
            @Override
            public void completed(AsynchronousSocketChannel socketChannel, AsynchronousServerSocketChannel serverSocketChannel) {
                serverSocketChannel.accept(serverSocketChannel, this);

                try {
                    saveChannel(socketChannel);
                    read(socketChannel);
                } catch (IOException e) {
                    // TODO: Handle save channel error.
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, AsynchronousServerSocketChannel attachment) {
                // TODO: Handle accept failed.
            }
        });

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write to client.
     * @param payload
     */
    private void write(AsynchronousSocketChannel socketChannel, final String payload) {
        socketChannel.write(ByteBuffer.wrap(payload.getBytes()), socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer bytesWritten, AsynchronousSocketChannel socketChannel) {
                if(bytesWritten == -1) failed(new Exception("Failed to write buffer."), socketChannel);
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel socketChannel) {
                System.out.println("[Server] Write failed.");
            }
        });
    }

    /**
     * Write to client.
     * @param payload
     */
    private void write(AsynchronousSocketChannel socketChannel, Payload payload) {
        write(socketChannel, payload.toString());
    }

    /**
     * Write to all clients.
     * @param payload
     */
    private void broadcast(Payload payload) {
        for(String address : channels.keySet()) {
            write(getChannelByAddress(address), payload);
        }
    }

    /**
     * Read from client.
     */
    private void read(AsynchronousSocketChannel socketChannel) {
        final ByteBuffer readBuffer = ByteBuffer.allocate(4096);
        socketChannel.read(readBuffer, socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer bytesRead, AsynchronousSocketChannel socketChannel) {
                if(bytesRead != -1) {
                    Payload payload = new Payload(readBuffer, bytesRead);
                    payload.appendSocketHeaders(socketChannel);
                    System.out.println("[Server] Read: " + payload);

                    notifyObservers("server:read", socketChannel, payload);

                    read(socketChannel);
                } else {
                    failed(new Exception("Failed to read buffer."), socketChannel);
                }
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel socketChannel) {
                close(socketChannel);
            }
        });
    }

    private InetSocketAddress getSocketAddress(AsynchronousSocketChannel socketChannel) throws IOException {
        return (InetSocketAddress) socketChannel.getRemoteAddress();
    }

    private String getSocketHostAddress(AsynchronousSocketChannel socketChannel) throws IOException {
        return getSocketAddress(socketChannel).getAddress().getHostAddress();
    }

    private void saveChannel(AsynchronousSocketChannel socketChannel) throws IOException {
        channels.put(getSocketHostAddress(socketChannel), socketChannel);
    }

    private AsynchronousSocketChannel getChannel(AsynchronousSocketChannel socketChannel) throws IOException {
        return channels.get(getSocketHostAddress(socketChannel));
    }

    private AsynchronousSocketChannel getChannelByAddress(String address) {
        return channels.get(address);
    }

    private void deleteChannel(AsynchronousSocketChannel socketChannel) throws IOException {
        channels.remove(getSocketHostAddress(socketChannel));
    }

    private void deleteChannelByAddress(String address) {
        channels.remove(address);
    }

    private void close(AsynchronousSocketChannel socketChannel) {
        try {
            String address = getSocketHostAddress(socketChannel);
            deleteChannelByAddress(address);

            System.out.println("[Server] Connection closed: " + address);

            notifyObservers("server:client:disconnected", null, address);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeAll() {
        for(String hostname : channels.keySet()) {
            close(getChannelByAddress(hostname));
        }
    }

    @Override
    public void update(String eventType, Object oldValue, Object newValue) {
        //System.out.println("[Server] Server event: " + eventType);

        switch (eventType) {
            case "ranking:calculate:max:rank:done": {
                String address = (String) oldValue;
                long rank = (long) newValue;

                Payload payload = new Payload();
                payload.setHeaderType("ranking:max:rank");

                JSONObject body = new JSONObject();
                body.put("rank", rank);
                body.put("address", address);
                payload.setBody(body);

                broadcast(payload);
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
