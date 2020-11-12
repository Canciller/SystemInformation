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

public class Server extends Observable {
    /**
     * Server constructor.
     */
    public Server() {
        super();
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
        final AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open().bind(socketAddress);

        System.out.format("Servidor abierto en %s:%d\n", host, port);

        // Start to accept connections from clients.
        serverSocketChannel.accept(serverSocketChannel, new CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {
            @Override
            public void completed(AsynchronousSocketChannel result, AsynchronousServerSocketChannel attachment) {
                try {
                    System.out.format("Conexión aceptada %s\n", result.getRemoteAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // A connection is accepted, start to accept next connection.
                serverSocketChannel.accept(serverSocketChannel, this);

                // Start to read message from current client.
                read(result);
            }

            @Override
            public void failed(Throwable exc, AsynchronousServerSocketChannel attachment) {
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

                    String payload = new String(buffer.array(), StandardCharsets.UTF_8);

                    // TODO: Handle parse error.
                    JSONObject object = new JSONObject(payload);
                    object.put("hostname", socketAddress.getHostName());

                    Server.this.notify(object.getString("type"), null, object);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // Start to read next message again.
                    if(socketChannel.isOpen())
                        read(socketChannel);
                }
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
                try {
                    InetSocketAddress socketAddress = (InetSocketAddress) attachment.getRemoteAddress();

                    String type = "client:disconnected";

                    JSONObject object = new JSONObject();
                    object.put("hostname", socketAddress.getHostName());
                    object.put("type", type);

                    Server.this.notify(type, null, object);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Write buffer to client.
     * @param socketChannel
     * @param buff
     */
    private void write(AsynchronousSocketChannel socketChannel, final ByteBuffer buff) {

    }

}
