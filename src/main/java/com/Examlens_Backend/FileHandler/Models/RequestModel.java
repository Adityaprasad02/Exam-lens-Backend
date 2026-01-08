package com.Examlens_Backend.FileHandler.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestModel {

    private List<TopicDetails> request ;
}
