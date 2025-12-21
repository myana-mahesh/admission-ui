package com.impactsure.sanctionui.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollegeForm {
    private Long collegeId;
    private String code;
    private String name;
    private List<Long> courseIds = new ArrayList<>();
    private List<Integer> totalSeats = new ArrayList<>();
}
