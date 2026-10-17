package com.lucky.quota.common.enums;

import com.lucky.quota.common.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: Lucky
 * @url: www.quanxiaoha.com
 * @date: 2023-08-15 10:33
 * @description: 响应异常码
 **/
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("10001", "参数错误"),


    // ----------- 业务异常状态码 -----------

    // ----------- 用户模块异常状态码 -----------
    USER_MOBILE_EXISTS("20001", "该手机号已注册"),
    USER_VERIFY_CODE_ERROR("20002", "验证码错误"),
    USER_MOBILE_NOT_REGISTERED("20003", "该手机号未注册"),
    USER_PASSWORD_ERROR("20004", "密码错误"),
    USER_STATUS_DISABLED("20005", "账号已被禁用，请联系管理员"),
    VERIFY_CODE_TYPE_ERROR("20006", "验证码类型错误"),
    VERIFY_CODE_SEND_TOO_FREQUENT("20007", "验证码发送过于频繁，请稍后再试"),
    VERIFY_CODE_DAILY_LIMIT_EXCEEDED("20008", "验证码每日发送次数已达上限，请明天再试"),
    LOGIN_FAIL_TOO_MANY("20009", "密码错误次数过多，请 30 分钟后再试"),
    USER_LOGIN_CREDENTIAL_ERROR("20011", "手机号或密码错误"),
    CAPTCHA_VERIFICATION_FAILED("20012", "行为验证码校验失败，请重新验证"),
    PERMISSION_DENIED("20013", "权限不足"),
    USER_NOT_LOGIN("20014", "用户未登录，请先登录"),
    USER_PERMISSION_DENIED("20015", "该用户没有权限"),
    USER_CANNOT_DELETE_ADMIN("20016", "不能删除管理员账号"),
    USER_CANNOT_DELETE_SELF("20017", "不能删除自己的账号"),
    USER_CANNOT_CANCEL_ADMIN("20018", "管理员不能注销自己"),
    BALANCE_NOT_ENOUGH("20019", "账户余额不足"),
    BALANCE_RECHARGE_FAIL("20020", "余额充值失败"),

    // ----------- 发放单位模块异常状态码 -----------
    SHOP_COUNT_LIMIT_EXCEEDED("30001", "发放单位数量已达上限，最多可创建 3 个"),
    SHOP_NOT_EXIST("30002", "发放单位不存在"),

    // ----------- 物资模块异常状态码 -----------
    GOODS_NOT_EXIST("35001", "物资不存在"),

    // ----------- 收藏模块异常状态码 -----------
    COLLECT_ALREADY_EXISTS("36001", "已收藏，请勿重复操作"),
    COLLECT_NOT_EXISTS("36002", "未收藏，无法取消"),

    // ----------- 库存模块异常状态码 -----------
    STOCK_NOT_ENOUGH("40001", "库存不足"),
    STOCK_DEDUCT_FAIL("40002", "库存扣减失败"),

    // ----------- 申领单模块异常状态码 -----------
    ORDER_STATUS_ERROR("50001", "申领单状态异常"),
    ORDER_EXPIRED("50002", "申领已过期"),
    ORDER_NOT_EXIST("50003", "申领单不存在"),
    SEATA_DEMO_ROLLBACK("50004", "已回滚"),

    // ----------- 限量申领模块异常状态码 -----------
    QUOTA_ACTIVITY_NOT_EXIST("60001", "限量场次不存在"),
    QUOTA_ACTIVITY_NOT_STARTED("60002", "限量场次未开始"),
    QUOTA_ACTIVITY_ENDED("60003", "限量场次已结束"),
    QUOTA_GOODS_NOT_EXIST("60004", "限量物资不存在"),
    QUOTA_LIMIT_EXCEEDED("60005", "超过限领数量"),
    QUOTA_REPEAT("60006", "您已申领过该物资"),
    QUOTA_BUSY("60007", "申领太火爆了，请稍后再试"),
    QUOTA_ORDER_CREATE_FAIL("60008", "限量申领失败"),
    QUOTA_ACTIVITY_ALREADY_STARTED("60009", "限量场次已在进行中"),
    QUOTA_ACTIVITY_NO_GOODS("60010", "该场次未配置限量物资"),
    QUOTA_GOODS_APPLY_PENDING("60011", "该规格限量投放申请审核中"),
    QUOTA_GOODS_ALREADY_EXISTS("60012", "该规格已是限量物资"),
    QUOTA_GOODS_NOT_APPROVED("60013", "限量物资未通过审核"),
    QUOTA_GOODS_USE_QUOTA_CHANNEL("60014", "该物资正在限量申领中，请前往限量申领页"),

    ;

    // 异常码
    private String errorCode;
    // 错误信息
    private String errorMessage;

}
