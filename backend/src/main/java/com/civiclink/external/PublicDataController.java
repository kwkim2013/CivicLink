package com.civiclink.external;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public-data/complaints")
public class PublicDataController {

    private final PublicDataClient publicDataClient;

    public PublicDataController(PublicDataClient publicDataClient) {
        this.publicDataClient = publicDataClient;
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public String list(
            @RequestParam(name = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(name = "numOfRows", defaultValue = "10") int numOfRows,
            @RequestParam(name = "search", defaultValue = "") String search,
            @RequestParam(name = "reqFrom", defaultValue = "20220301") String reqFrom,
            @RequestParam(name = "reqTo", defaultValue = "20220310") String reqTo) {
        return publicDataClient.getComplaintList(pageNo, numOfRows, search, reqFrom, reqTo);
    }

    @GetMapping(value = "/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public String detail(@RequestParam(name = "docMngNo") long docMngNo) {
        return publicDataClient.getComplaintDetail(docMngNo);
    }
}
