package com.dist.system.info.server;

import com.dist.system.info.Main;
import com.dist.system.info.util.Observer;
import com.dist.system.info.util.Payload;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Server extends Observer implements Runnable {
    public static String connectedHost;
    public static String serverHost;

    String host;
    int port;

    Queue<String> benchmarkQueue;

    ConcurrentHashMap<String, AsynchronousSocketChannel> channels;
    ConcurrentHashMap<String, Future<Integer>> writeFutures;

    AsynchronousServerSocketChannel serverSocketChannel;

    ReentrantLock writeServerSwitchMutex;
    AtomicInteger broadcastClients;

    AtomicLong rankingStart;
    final long rankingWait = 10; // seconds

    /**
     * Server constructor.
     * @param host
     * @param port
     */
    public Server(String host, int port) {
        super();

        this.host = host;
        this.port = port;

        serverHost = host;

        benchmarkQueue = new ConcurrentLinkedQueue<String>();

        channels = new ConcurrentHashMap<>();
        writeFutures = new ConcurrentHashMap<>();

        writeServerSwitchMutex = new ReentrantLock();
        broadcastClients = new AtomicInteger(0);

        rankingStart = new AtomicLong(System.nanoTime());
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

                restartRankingTimer();

                try {
                    if(connectedHost.equals(host)) {
                        saveChannel(socketChannel);
                        read(socketChannel);
                    } else {
                        Payload serverSwitchPayload = new Payload();
                        serverSwitchPayload.setHeaderType("server:switch");

                        JSONObject body = new JSONObject();
                        body.put("address", connectedHost);

                        serverSwitchPayload.setBody(body);

                        writeServerSwitch(socketChannel, serverSwitchPayload);
                    }
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
                else broadcastClients.incrementAndGet();
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel socketChannel) {
                //System.out.println("[Server] Write failed.");
                broadcastClients.incrementAndGet();
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


    private void writeServerSwitch(AsynchronousSocketChannel socketChannel, Payload payload) {
        String message = payload.toString();

        writeServerSwitchMutex.lock();

        socketChannel.write(ByteBuffer.wrap(message.getBytes()), socketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>() {
            @Override
            public void completed(Integer bytesWritten, AsynchronousSocketChannel socketChannel) {
                if(bytesWritten == -1) failed(new Exception("Failed to write buffer."), socketChannel);
                unlockServerSwitchMutex();
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel socketChannel) {
                //System.out.println("[Server] Write failed.");
                unlockServerSwitchMutex();
            }
        });
    }

    /**
     * Write to all clients.
     * @param payload
     */
    private void broadcast(Payload payload) {
        writeServerSwitchMutex.lock();
        broadcastClients.set(0);

        for(String address : channels.keySet()) {
            write(getChannelByAddress(address), payload);
        }

        int totalChannels = channels.size();
        while(broadcastClients.get() != totalChannels);

        unlockServerSwitchMutex();
    }

    /**
     * Read from client.
     * @param socketChannel
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

                    if(payload.getHeaderType().equals("benchmark")) {
                        System.out.println("[Server] Benchmark added.");
                        benchmarkQueue.add("benchmark");
                    }

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
            //socketChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeAll() {
        for(String hostname : channels.keySet()) {
            close(getChannelByAddress(hostname));
        }
    }

    void unlockServerSwitchMutex() {
        if(writeServerSwitchMutex.isHeldByCurrentThread() && writeServerSwitchMutex.isLocked())
            writeServerSwitchMutex.unlock();
    }

    void restartRankingTimer() {
        long currRankingStart = rankingStart.getAndSet(System.nanoTime());
        System.out.println("[Server] Ranking timer restarted.");
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
        Thread rankingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    long currRankingStart = rankingStart.get();
                    long rankingEnd = System.nanoTime();

                    long duration = TimeUnit.NANOSECONDS.toSeconds(rankingEnd - currRankingStart);

                    //System.out.println("[Server] Ranking timer: " + duration);

                    if(duration >= rankingWait) {
                        restartRankingTimer();
                        notifyObservers("ranking:calculate:max:rank", null, null);
                    }
                }
            }
        });

        rankingThread.start();

        Thread benchmarkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if(!benchmarkQueue.isEmpty()) {
                        benchmarkQueue.poll();
                        try {
                            String [] command = {
                                "winsat",
                                "formal"
                            };
                            Process process = Runtime.getRuntime().exec(command);

                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(process.getInputStream()));
                            String line = "";
                            while (true) {
                                try {
                                    if (!((line = reader.readLine()) != null)) break;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                System.out.println(line);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        benchmarkThread.start();

        try {
            start();
        } catch (IOException e) {
            // TODO: Handle error.
            e.printStackTrace();
        }
    }
}
