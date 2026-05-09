package com.nidextractor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private T data;

    @JsonProperty("error")
    private String error;

    // ── Static factories ──────────────────────────────────────
    public static <T> ApiResponse<T> ok(T data, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.message = message;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> error(String error) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.error = error;
        return r;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData()         { return data; }
    public String getError()   { return error; }
}