package tech.tresearchgroup.libraries.authentication.controller;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class OAuthHandler implements HttpHandler {
    private final HttpServer server;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final OAuth20Service service;
    private String authCode = null;

    public OAuthHandler(DefaultApi20 defaultApi20) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 60842), 0);
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        server.createContext("/code", this);
        server.setExecutor(threadPoolExecutor);
        server.start();
        service = new ServiceBuilder("f53ca19b7a73459bb76b97184fa2470b")
            .apiSecret("Qm8ygwBClloiNirkItOv294rNpCjYTqc")
            .callback("http://localhost:60842/code")
            .build(defaultApi20);
        String authorizationUrl = service.getAuthorizationUrl();
        System.out.println(authorizationUrl);
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String requestParamValue = null;
        if ("GET".equals(httpExchange.getRequestMethod())) {
            requestParamValue = handleGetRequest(httpExchange);
        }
        try {
            handleResponse(httpExchange, requestParamValue);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String handleGetRequest(HttpExchange httpExchange) {
        return httpExchange.
            getRequestURI()
            .toString()
            .split("\\?")[1]
            .split("=")[1];
    }

    private void handleResponse(HttpExchange httpExchange, String requestParamValue) throws IOException, ExecutionException, InterruptedException {
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(200, 0);
        outputStream.flush();
        outputStream.close();
        server.stop(0);
        threadPoolExecutor.shutdown();
        OAuth2AccessToken accessToken = service.getAccessToken(requestParamValue);
        service.close();
        authCode = accessToken.getAccessToken();
    }

    public String getAuthCode() throws InterruptedException {
        while(authCode == null) {
            Thread.sleep(1000);
        }
        return authCode;
    }
}
