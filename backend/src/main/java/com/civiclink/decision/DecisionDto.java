package com.civiclink.decision;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/** 공개 결정례의 정규화 결과. 날짜와 원문은 제공된 그대로 보존한다. */
public class DecisionDto {
    public long documentNumber;
    public String title;
    public String category;
    public String decisionDate;
    public String respondent;
    public String body;
    public String requestContent;
    public String decisionContent;
    public String decisionType;
    public JsonNode footnotes;
    public JsonNode rawResponse;
    public String sourceUrl;
    public String collectedAt;
    public List<String> warnings = new ArrayList<>();
}
