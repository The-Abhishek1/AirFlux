package com.xcloak.airflux.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HttpClientProvider {
    // How long a pooled keep-alive connection to our own LocalFileServer is kept around.
    // Must stay in sync with the socket timeout LocalFileServer/NanoHTTPD is started with
    // (see SharingViewModel.startServer) — if the client holds a pooled connection longer
    // than the server is willing to keep it open, the next request on that connection fails
    // with a connection reset.
    const val SHARE_KEEP_ALIVE_MILLIS = 2 * 60 * 1000L // 2 minutes

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                    .build()
                chain.proceed(request)
            }
            .connectionPool(okhttp3.ConnectionPool(5, SHARE_KEEP_ALIVE_MILLIS, TimeUnit.MILLISECONDS))
            .build()
    }
}