package com.civiclink.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class PublicDataClient {

    private final RestTemplate restTemplate;
    private final String serviceKey;
    private final String listUrl;
    private final String detailUrl;

    public PublicDataClient(
            RestTemplate restTemplate,
            @Value("${publicdata.service-key}") String serviceKey,
            @Value("${publicdata.complaint.list-url}") String listUrl,
            @Value("${publicdata.complaint.detail-url}") String detailUrl) {
        this.restTemplate = restTemplate;
        this.serviceKey = serviceKey;
        this.listUrl = listUrl;
        this.detailUrl = detailUrl;
    }

    public String getComplaintList(int pageNo, int numOfRows,
                                   String search, String reqFrom, String reqTo) {
        URI uri = UriComponentsBuilder.fromHttpUrl(listUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("search", UriUtils.encodeQueryParam(search, StandardCharsets.UTF_8))
                .queryParam("reqFrom", reqFrom)
                .queryParam("reqTo", reqTo)
                .queryParam("gbn", "1")
                .queryParam("pageSize", numOfRows)
                .queryParam("pageIndex", pageNo)
                .build(true)
                .toUri();

        return restTemplate.getForObject(uri, String.class);
    }

    public String getComplaintDetail(long docMngNo) {
        URI uri = UriComponentsBuilder.fromHttpUrl(detailUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("docMngNo", docMngNo)
                .queryParam("gbn", "1")
                .build(true)
                .toUri();

        return restTemplate.getForObject(uri, String.class);
    }
}
