package codes.rafael.interceptablehttpclient;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiConsumer;
import java.util.function.Function;

class Interceptor<T> {

    private final Function<HttpRequest, T> onRequest;

    private final BiConsumer<HttpResponse<?>, T> onResponse;

    private final BiConsumer<Throwable, T> onError;

    Interceptor(Function<HttpRequest, T> onRequest, BiConsumer<HttpResponse<?>, T> onResponse, BiConsumer<Throwable, T> onError) {
        this.onRequest = onRequest;
        this.onResponse = onResponse;
        this.onError = onError;
    }

    Function<HttpRequest, T> getOnRequest() {
        return onRequest;
    }

    BiConsumer<HttpResponse<?>, T> getOnResponse() {
        return onResponse;
    }

    BiConsumer<Throwable, T> getOnError() {
        return onError;
    }
}
