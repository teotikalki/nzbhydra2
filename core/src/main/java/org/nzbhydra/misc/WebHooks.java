package org.nzbhydra.misc;

import com.google.common.base.Strings;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.nzbhydra.Jackson;
import org.nzbhydra.downloading.FileDownloadEvent;
import org.nzbhydra.okhttp.HydraOkHttp3ClientHttpRequestFactory;
import org.nzbhydra.searching.Searcher.SearchEvent;
import org.nzbhydra.searching.searchrequests.SearchRequest.SearchSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class WebHooks {

    private static final Logger logger = LoggerFactory.getLogger(WebHooks.class);

    @Autowired
    private HydraOkHttp3ClientHttpRequestFactory requestFactory;

    @Async
    @EventListener
    public void onSearchEvent(SearchEvent searchEvent) throws IOException {
        String searchHook = System.getProperty("nzbhydra.hooks.search");
        if (!Strings.isNullOrEmpty(searchHook)) {
            if (searchEvent.getSearchRequest().getSource() == SearchSource.INTERNAL) {
                try {
                    OkHttpClient client = requestFactory.getOkHttpClientBuilder(URI.create(searchHook)).build();
                    String content = Jackson.JSON_MAPPER.writeValueAsString(searchEvent.getSearchRequest());
                    Response response = client.newCall(new Builder().url(searchHook).method("PUT", RequestBody.create(MediaType.parse(org.springframework.http.MediaType.APPLICATION_JSON_VALUE), content)).build()).execute();
                    response.close();

                    logger.debug("Called search web hook with response {}", response);
                } catch (IOException e) {
                    logger.error("Unable to execute webhook to {} on search event", searchHook);
                }
            }
        }
    }

    @Async
    @EventListener
    public void onNzbDownloadEvent(FileDownloadEvent downloadEvent) throws IOException {
        String downloadHook = System.getProperty("nzbhydra.hooks.download");
        if (!Strings.isNullOrEmpty(downloadHook)) {
            if (downloadEvent.getDownloadEntity().getAccessSource() == SearchSource.INTERNAL) {
                try {
                    OkHttpClient client = requestFactory.getOkHttpClientBuilder(URI.create(downloadHook)).build();
                    String content = Jackson.JSON_MAPPER.writeValueAsString(downloadEvent.getDownloadEntity());
                    Response response = client.newCall(new Builder().url(downloadHook).method("PUT", RequestBody.create(MediaType.parse(org.springframework.http.MediaType.APPLICATION_JSON_VALUE), content)).build()).execute();
                    response.close();

                    logger.debug("Called download web hook with response {}", response);
                } catch (IOException e) {
                    logger.error("Unable to execute webhook to {} on download event", downloadHook);
                }
            }
        }
    }


}
