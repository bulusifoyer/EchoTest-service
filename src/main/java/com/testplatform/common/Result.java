package com.testplatform.common;

import lombok.Data;

/**
 * 统一API响应封装类
 * 用于统一返回前端的数据格式，包含状态码、消息和数据
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Data
public class Result<T> {

    /**
     * 响应状态码
     * 200 - 请求成功
     * 400 - 请求参数错误
     * 401 - 未授权
     * 500 - 服务器内部错误
     */
    private Integer code;

    /**
     * 响应消息
     * 用于描述请求的处理结果
     */
    private String message;

    /**
     * 响应数据
     * 泛型设计，可以返回任意类型的数据
     */
    private T data;

    /**
     * 私有化构造方法
     * 强制使用静态方法创建实例
     */
    private Result() {
    }

    /**
     * 私有化构造方法
     * @param code 状态码
     * @param message 消息
     * @param data 数据
     */
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 请求成功，无数据返回
     * @return 统一响应对象
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 请求成功，有数据返回
     * @param data 返回数据
     * @return 统一响应对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 请求成功，自定义消息
     * @param message 成功消息
     * @param data 返回数据
     * @return 统一响应对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 请求失败，默认消息
     * @return 统一响应对象
     */
    public static <T> Result<T> error() {
        return new Result<>(500, "操作失败", null);
    }

    /**
     * 请求失败，自定义消息
     * @param message 错误消息
     * @return 统一响应对象
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 请求失败，自定义状态码和消息
     * @param code 错误状态码
     * @param message 错误消息
     * @return 统一响应对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 请求失败，自定义状态码、消息和数据
     * @param code 错误状态码
     * @param message 错误消息
     * @param data 错误数据
     * @return 统一响应对象
     */
    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
}