package com.justjava.devFlow.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class PlantUmlRequest {

    @NotBlank(message = "PlantUML source code is required")
    @Size(max = 10000, message = "PlantUML source too long (max 10000 characters)")
    private String source;

    private String filename;

    // Constructors
    public PlantUmlRequest() {}

    public PlantUmlRequest(String source) {
        this.source = source;
    }

}
