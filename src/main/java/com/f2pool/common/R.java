package com.f2pool.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(500, msg, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public static <T> R<T> fail(HttpStatus status, String msg) {
        return new R<>(status.value(), msg, null);
    }

    public static <T> R<T> badRequest(String msg) {
        return fail(HttpStatus.BAD_REQUEST, msg);
    }

    public static <T> R<T> unauthorized(String msg) {
        return fail(HttpStatus.UNAUTHORIZED, msg);
    }

    public static <T> R<T> forbidden(String msg) {
        return fail(HttpStatus.FORBIDDEN, msg);
    }

    public static <T> R<T> notFound(String msg) {
        return fail(HttpStatus.NOT_FOUND, msg);
    }

    public static <T> R<T> conflict(String msg) {
        return fail(HttpStatus.CONFLICT, msg);
    }

    public static <T> R<T> serverError(String msg) {
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, msg);
    }
}
