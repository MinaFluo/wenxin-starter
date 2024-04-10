package com.gearwenxin.common;

import lombok.Getter;

/**
 * 错误码
 *
 * @author Ge Mingjia
 * {@code @date} 2023/7/22
 */
@Getter
public enum ErrorCode {

    PARAMS_ERROR(40000, "参数错误"),
    REQUEST_TYPE_ERROR(40001, "不受支持的请求类"),
    NO_AUTH_ERROR(40101, "无权限"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    SYSTEM_NET_ERROR(50002, "系统网络异常"),
    WENXIN_ERROR(1, "响应异常"),
    SYSTEM_INPUT_ERROR(336104, "'用户输入错误' system内容不合法"),
    EVENT_LOOP_ERROR(50003, "事件循环异常"),
    CONSUMER_THREAD_START_FAILED(50004, "消费者线程启动失败"),
    ;

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}