package com.example.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String to;
    private String type;
    private String link;
}