package com.icr.atlas.rp1.utils;

public class ApiResponse {
    private int status;
    private String body;

    public ApiResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }
}