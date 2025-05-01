package com.tastyrecipes.application.dto;


import lombok.*;


@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;

    public ApiResponse(boolean b, String message) {
        this.success=b;
        this.message=message;
    }
}