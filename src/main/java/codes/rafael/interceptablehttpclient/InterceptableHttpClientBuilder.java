package codes.rafael.interceptablehttpclient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

class InterceptableHttpClientBuilder implements InterceptableHttpClient.Builder {

    private final HttpClient.Builder builder = HttpClient.newBuilder();

    private final List<Consumer<HttpRequest.Builder>> decorators = new ArrayList<>();

    private final List<Interceptor<?>> interceptors = new ArrayList<>();

    @Override
    public InterceptableHttpClient.Builder cookieHandler(CookieHandler cookieHandler) {
        builder.cookieHandler(cookieHandler);
        return this;
    }

    @Override
    public InterceptableHttpClient.Builder connectTimeout(Duration duration) {
        builder.connectTimeout(duration);
        return this;
    }

    @Override
    public InterceptableHttpClient.Builder sslContext(SSLContext sslContext) {
        builder.sslContext(sslContext);
        return this;
    }

    @Override
    public InterceptableHttpClient.Builder sslParameters(SSLParameters sslParameters) {
        builder.sslParameters(sslParameters);
        return this;
    }

    @Override
    public InterceptableHttpClient.Builder executor(Executor executor) {
        builder.executor(executor);
        return this;
    }

    @Override
    public InterceptableHttpClient.Builder followRedirects(HttpClient.Redirect redirect) {
        builder.followRedirects(redirect);
        return this;
    }

    @Override
    public InterceptableHttpClient.Builder version(HttpClient.Version version) {
        builder.version(version);
        return this;
    }

    @Override
    public InterceptableHttpClient.Builder priority(int priority) {
        builder.priority(priority);
        return this;
    }

    @Override
    public InterceptableHttpClient.Builder proxy(ProxySelector proxySelector) {
        builder.proxy(proxySelector);
        return this;
    }

    @Override
    public InterceptableHttpClient.Builder authenticator(Authenticator authenticator) {
        builder.authenticator(authenticator);
        return this;
    }

    @Override
    public InterceptableHttpClient.Builder decorator(Consumer<HttpRequest.Builder> decorator) {
        Objects.requireNonNull(decorator, "decorator");
        decorators.add(decorator);
        return this;
    }

    @Override
    public <T> InterceptableHttpClient.Builder interceptor(
            Function<HttpRequest, T> onRequest,
            BiConsumer<HttpResponse<?>, T> onResponse,
            BiConsumer<Throwable, T> onError) {
        Objects.requireNonNull(onRequest, "onRequest");
        Objects.requireNonNull(onResponse, "onResponse");
        Objects.requireNonNull(onError, "onError");
        interceptors.add(new Interceptor<>(onRequest, onResponse, onError));
        return this;
    }

    @Override
    public HttpClient build() {
        return new InterceptableHttpClient(builder.build(), new ArrayList<>(decorators), new ArrayList<>(interceptors));
    }
}
