package com.dist.system.info.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Iterator;

public class Payload {
    JSONObject headers;
    JSONObject body;

    public Payload() {
        this(new JSONObject(), new JSONObject());
    }

    public Payload(JSONObject headers, JSONObject body) {
        this.headers = headers == null ? new JSONObject() : headers;
        this.body = body == null ? new JSONObject() : body;
    }

    public Payload(String payload) {
        setFromString(payload);
    }

    public Payload(ByteBuffer byteBuffer, int bytesRead) {
        byteBuffer.flip();

        byte[] bytes = new byte[bytesRead];
        byteBuffer.get(bytes, 0, bytesRead);
        String payload = new String(bytes);

        setFromString(payload);
    }

    public void setFromString(String payload) {
        JSONObject headers = new JSONObject(),
                body = new JSONObject();

        try {
            JSONObject pl = new JSONObject(payload.trim());

            if(pl.has("headers"))
                headers = pl.getJSONObject("headers");

            if(pl.has("body"))
                body = pl.getJSONObject("body");
        } catch (JSONException e) {}

        this.headers = headers;
        this.body = body;
    }

    public JSONObject setSocketAddressHeaders(InetSocketAddress socketAddress) {
        if(socketAddress == null) return headers;

        setHeader("hostname", socketAddress.getHostName());
        setHeader("address", socketAddress.getAddress().getHostAddress());

        return headers;
    }

    public JSONObject appendSocketHeaders(AsynchronousSocketChannel socketChannel) {
        try {
            InetSocketAddress socketAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            setSocketAddressHeaders(socketAddress);
        } catch (IOException e) {
            return headers;
        }

        return headers;
    }

    public JSONObject getHeaders() {
        return headers;
    }

    public String getHeaderHostname() {
        return (String) getHeader("hostname");
    }

    public String getHeaderAddress() {
        return (String) getHeader("address");
    }

    public void setHeaderType(String type) {
        setHeader("type", type);
    }

    public String getHeaderType() {
        return (String) getHeader("type");
    }

    public JSONObject setHeader(String key, Object value) {
        if(headers != null)
            headers.put(key, value);

        return headers;
    }

    public Object getHeader(String key) {
        if(headers != null && headers.has(key))
            return headers.get(key);

        return null;
    }

    public JSONObject appendHeaders(JSONObject headers) {
        if(headers == null) return this.headers;

        Iterator<String> keys = headers.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            setHeader(key, headers.get(key));
        }

        return this.headers;
    }

    public JSONObject getBody() {
        return body;
    }

    public JSONObject setBody(JSONObject body) {
        this.body = body;

        return this.body;
    }

    public void clearBody() {
        body = new JSONObject();
    }

    @Override
    public String toString() {
        JSONObject payload = new JSONObject();

        payload.put("headers", headers);
        payload.put("body", body);

        return payload.toString();
    }
}
