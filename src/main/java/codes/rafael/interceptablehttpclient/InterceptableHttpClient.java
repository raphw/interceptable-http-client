package codes.rafael.interceptablehttpclient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class InterceptableHttpClient extends HttpClient {

    private final HttpClient client;

    private final List<Consumer<HttpRequest.Builder>> decorators;

    private final List<Interceptor<?>> interceptors;

    public static Builder builder() {
        return new InterceptableHttpClientBuilder();
    }

    InterceptableHttpClient(
            HttpClient client,
            List<Consumer<HttpRequest.Builder>> decorators,
            List<Interceptor<?>> interceptors) {
        this.client = client;
        this.decorators = decorators;
        this.interceptors = interceptors;
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return client.cookieHandler();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return client.connectTimeout();
    }

    @Override
    public Redirect followRedirects() {
        return client.followRedirects();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return client.proxy();
    }

    @Override
    public SSLContext sslContext() {
        return client.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return client.sslParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return client.authenticator();
    }

    @Override
    public Version version() {
        return client.version();
    }

    @Override
    public Optional<Executor> executor() {
        return client.executor();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> HttpResponse<T> send(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
        httpRequest = decorate(httpRequest);
        if (interceptors.isEmpty()) {
            return client.send(httpRequest, bodyHandler);
        } else {
            List<Object> values = new ArrayList<>(interceptors.size());
            for (Interceptor<?> interceptor : interceptors) {
                try {
                    values.add(interceptor.getOnRequest().apply(httpRequest));
                } catch (Throwable ignored) { }
            }
            try {
                HttpResponse<T> response = client.send(httpRequest, bodyHandler);
                for (int index = 0; index < interceptors.size(); index++) {
                    BiConsumer onResponse = interceptors.get(index).getOnResponse();
                    try {
                        onResponse.accept(response, values.get(index));
                    } catch (Throwable ignored) { }
                }
                return response;
            } catch (IOException exception) {
                for (int index = 0; index < interceptors.size(); index++) {
                    BiConsumer onResponse = interceptors.get(index).getOnError();
                    try {
                        onResponse.accept(exception, values.get(index));
                    } catch (Throwable ignored) { }
                }
                throw exception;
            }
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) {
        return sendAsync(httpRequest, bodyHandler, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        httpRequest = decorate(httpRequest);
        if (interceptors.isEmpty()) {
            return client.sendAsync(httpRequest, bodyHandler);
        } else {
            List<Object> values = new ArrayList<>(interceptors.size());
            for (Interceptor<?> interceptor : interceptors) {
                values.add(interceptor.getOnRequest().apply(httpRequest));
            }
            return client.sendAsync(httpRequest, bodyHandler, pushPromiseHandler).handle((response, throwable) -> {
                for (int index = 0; index < interceptors.size(); index++) {
                    if (throwable == null) {
                        BiConsumer onResponse = interceptors.get(index).getOnResponse();
                        onResponse.accept(response, values.get(index));
                    } else {
                        BiConsumer onResponse = interceptors.get(index).getOnError();
                        onResponse.accept(throwable, values.get(index));
                    }
                }
                return response;
            });
        }
    }

    private HttpRequest decorate(HttpRequest httpRequest) {
        if (decorators.isEmpty()) {
            return httpRequest;
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(httpRequest.uri());
        builder.expectContinue(httpRequest.expectContinue());
        httpRequest.headers().map().forEach((key, values) -> values.forEach(value -> builder.header(key, value)));
        httpRequest.bodyPublisher().ifPresentOrElse(
                publisher -> builder.method(httpRequest.method(), publisher),
                () -> {
                    switch (httpRequest.method()) {
                        case "GET":
                            builder.GET();
                            break;
                        case "DELETE":
                            builder.DELETE();
                            break;
                        default:
                            throw new IllegalStateException(httpRequest.method());
                    }
                }
        );
        httpRequest.timeout().ifPresent(builder::timeout);
        httpRequest.version().ifPresent(builder::version);
        for (Consumer<HttpRequest.Builder> decorator : decorators) {
            decorator.accept(builder);
        }
        return builder.build();
    }

    public interface Builder extends HttpClient.Builder {

        @Override
        Builder cookieHandler(CookieHandler cookieHandler);

        @Override
        Builder connectTimeout(Duration duration);

        @Override
        Builder sslContext(SSLContext sslContext);

        @Override
        Builder sslParameters(SSLParameters sslParameters);

        @Override
        Builder executor(Executor executor);

        @Override
        Builder followRedirects(Redirect redirect);

        @Override
        Builder version(Version version);

        @Override
        Builder priority(int i);

        @Override
        Builder proxy(ProxySelector proxySelector);

        @Override
        Builder authenticator(Authenticator authenticator);

        Builder decorator(Consumer<HttpRequest.Builder> decorator);

        <T> Builder interceptor(Function<HttpRequest, T> onRequest, BiConsumer<HttpResponse<?>, T> onResponse, BiConsumer<Throwable, T> onError);
    }
}
