package codes.rafael.interceptablehttpclient;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class InterceptableHttpClientTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule();

    @Test
    public void can_decorate_request() throws URISyntaxException, IOException, InterruptedException {
        stubFor(get(urlEqualTo("/"))
            .withHeader("foo", equalTo("bar"))
            .willReturn(aResponse().withStatus(200).withBody("qux")));

        HttpClient client = InterceptableHttpClient.builder().decorator(builder -> builder.header("foo", "bar")).build();

        HttpResponse<String> response = client.send(HttpRequest.newBuilder(new URI("http://localhost:" + wireMock.port()))
            .GET()
            .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals("qux", response.body());
    }

    @Test
    public void can_decorate_async_request() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException {
        stubFor(get(urlEqualTo("/"))
            .withHeader("foo", equalTo("bar"))
            .willReturn(aResponse().withStatus(200).withBody("qux")));

        HttpClient client = InterceptableHttpClient.builder().decorator(builder -> builder.header("foo", "bar")).build();

        HttpResponse<String> response = client.sendAsync(HttpRequest.newBuilder(new URI("http://localhost:" + wireMock.port()))
            .GET()
            .build(), HttpResponse.BodyHandlers.ofString()).get(1, TimeUnit.SECONDS);

        assertEquals("qux", response.body());
    }

    @Test
    public void can_intercept_request() throws URISyntaxException, IOException, InterruptedException {
        stubFor(get(urlEqualTo("/")).willReturn(aResponse().withStatus(200).withBody("qux")));

        AtomicReference<String> value = new AtomicReference<>();
        HttpClient client = InterceptableHttpClient.builder().interceptor(
            builder -> "baz", (response, payload) ->  value.set(payload), (throwable, payload) -> fail()
        ).build();

        HttpResponse<String> response = client.send(HttpRequest.newBuilder(new URI("http://localhost:" + wireMock.port()))
            .GET()
            .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals("qux", response.body());

        assertEquals("baz", value.get());
    }

    @Test
    public void can_intercept_async_request() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException {
        stubFor(get(urlEqualTo("/")).willReturn(aResponse().withStatus(200).withBody("qux")));

        AtomicReference<String> value = new AtomicReference<>();
        HttpClient client = InterceptableHttpClient.builder().interceptor(
            builder -> "baz", (response, payload) ->  value.set(payload), (throwable, payload) -> fail()
        ).build();

        HttpResponse<String> response = client.sendAsync(HttpRequest.newBuilder(new URI("http://localhost:" + wireMock.port()))
            .GET()
            .build(), HttpResponse.BodyHandlers.ofString()).get(1, TimeUnit.SECONDS);

        assertEquals("qux", response.body());

        assertEquals("baz", value.get());
    }
}