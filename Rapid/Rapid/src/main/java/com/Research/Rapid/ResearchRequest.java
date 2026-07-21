package com.Research.Rapid;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Data

public class ResearchRequest {
    private String content;
    private String operation;

}
