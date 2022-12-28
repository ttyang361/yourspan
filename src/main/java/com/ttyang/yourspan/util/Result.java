package com.ttyang.yourspan.util;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-16
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@ApiModel(value = "全局统一返回结果")
public class Result<T> {
    @ApiModelProperty(value = "返回码")
    private Integer code;
    @ApiModelProperty(value = "返回消息")
    private String message;
    @ApiModelProperty(value = "返回数据")
    private T data;

    public static <T> Result<T> build(T data) {
        Result<T> result = new Result<>();
        if (data != null) {
            result.setData(data);
        }
        return result;
    }

    public static <T> Result<T> build(T data, ResultEnum resultEnum) {
        Result<T> result = build(data);
        result.setCode(resultEnum.getCode());
        result.setMessage(resultEnum.getMessage());
        return result;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return build(data, ResultEnum.SUCCESS);
    }

    public static <T> Result<T> fail() {
        return fail(null);
    }

    public static <T> Result<T> fail(T data) {
        return build(data, ResultEnum.FAIL);
    }

    public Result<T> message(String msg) {
        this.setMessage(msg);
        return this;
    }

    public Result<T> code(Integer code) {
        this.setCode(code);
        return this;
    }

    public boolean isOk() {
        return this.getCode().intValue() == ResultEnum.SUCCESS.getCode().intValue();
    }
}
