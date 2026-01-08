package com.Examlens_Backend.FileHandler.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = false)
public class ResponseModel {

    @JsonProperty("Topic")
    private String topic;

    @JsonProperty("Total_Marks")
    private Integer totalMarks;

    @JsonProperty("Important_Subtopics")
    private List<String> importantSubtopics;
}