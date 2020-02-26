Interception extension for Java's HttpClient
--------------------------------------------

This module adds a common feature of HTTP client implementations to Java 11's `java.net.http.HttpClient`:

- Adding request decorators, to for example add or modify request headers.
- Adding request interception, to for example collect request metrics.

The extension is straight-forward to use by simply replacing `HttpClient.builder` by `InterceptableHttpClient.builder()`. The extended builder API is then capable of registering decorators and interceptors by the two additional build steps.
  