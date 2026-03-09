package com.f2pool.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Map<String, String> MESSAGE_I18N_MAP = new LinkedHashMap<>();

    static {
        MESSAGE_I18N_MAP.put("Resource not found", "资源不存在");
        MESSAGE_I18N_MAP.put("Authorization header is required", "请求头缺少Authorization");
        MESSAGE_I18N_MAP.put("Invalid request", "请求参数不合法");
        MESSAGE_I18N_MAP.put("Method not allowed", "请求方法不允许");
        MESSAGE_I18N_MAP.put("Data conflict", "数据冲突");
        MESSAGE_I18N_MAP.put("System error", "系统异常");

        MESSAGE_I18N_MAP.put("request body is required", "请求参数不能为空");
        MESSAGE_I18N_MAP.put("file is required", "请上传文件");
        MESSAGE_I18N_MAP.put("userId is required", "用户ID不能为空");
        MESSAGE_I18N_MAP.put("id is required", "ID不能为空");
        MESSAGE_I18N_MAP.put("machineId is required", "矿机ID不能为空");
        MESSAGE_I18N_MAP.put("name is required", "名称不能为空");
        MESSAGE_I18N_MAP.put("hashrateValue must be greater than 0", "算力数值必须大于0");
        MESSAGE_I18N_MAP.put("hashrateUnit is required", "算力单位不能为空");
        MESSAGE_I18N_MAP.put("unsupported hashrate unit: ", "不支持的算力单位: ");
        MESSAGE_I18N_MAP.put("pricePerUnit must be greater than 0", "单价必须大于0");
        MESSAGE_I18N_MAP.put("lockDays must be greater than or equal to 0", "锁仓天数不能小于0");
        MESSAGE_I18N_MAP.put("hashrate value is required", "算力数值不能为空");
        MESSAGE_I18N_MAP.put("amount must be greater than 0", "金额必须大于0");
        MESSAGE_I18N_MAP.put("insufficient balance", "余额不足");
        MESSAGE_I18N_MAP.put("receiveAddress is required", "收款地址不能为空");
        MESSAGE_I18N_MAP.put("receive address already exists", "收款地址已存在");
        MESSAGE_I18N_MAP.put("receive address not found", "收款地址不存在");
        MESSAGE_I18N_MAP.put("please bind receive address before withdraw", "请先绑定收款地址后再提取");
        MESSAGE_I18N_MAP.put("fundPassword is required", "资金密码不能为空");
        MESSAGE_I18N_MAP.put("fund password not set, please set it in settings first", "未设置资金密码，请先在设置中配置");
        MESSAGE_I18N_MAP.put("fund password is incorrect", "资金密码错误");
        MESSAGE_I18N_MAP.put("user not found", "用户不存在");
        MESSAGE_I18N_MAP.put("network is required", "网络不能为空");
        MESSAGE_I18N_MAP.put("network must be one of TRC20/ERC20/BTC", "网络必须为TRC20/ERC20/BTC");
        MESSAGE_I18N_MAP.put("invalid asset/network pair", "资产与网络不匹配");
        MESSAGE_I18N_MAP.put("asset and network are required", "资产和网络不能为空");
        MESSAGE_I18N_MAP.put("asset is required", "资产不能为空");
        MESSAGE_I18N_MAP.put("asset must be one of USDT/USDC/BTC", "资产必须为USDT/USDC/BTC");
        MESSAGE_I18N_MAP.put("status is required", "状态不能为空");
        MESSAGE_I18N_MAP.put("status must be 1(approve) or 2(reject)", "状态必须为1(通过)或2(拒绝)");
        MESSAGE_I18N_MAP.put("status must be 0 or 1", "状态只能为0或1");
        MESSAGE_I18N_MAP.put("config key is required", "配置键不能为空");
        MESSAGE_I18N_MAP.put("config not found", "配置不存在");
        MESSAGE_I18N_MAP.put("recharge address config can only be modified in database", "收款地址配置仅允许在数据库中修改");

        MESSAGE_I18N_MAP.put("order not found", "订单不存在");
        MESSAGE_I18N_MAP.put("order does not belong to this user", "该订单不属于当前用户");
        MESSAGE_I18N_MAP.put("order is not in holding status", "订单不是持有中状态");
        MESSAGE_I18N_MAP.put("order with revenue cannot be canceled", "已有收益的订单不能取消");
        MESSAGE_I18N_MAP.put("no withdrawable revenue", "暂无可提取收益");
        MESSAGE_I18N_MAP.put("no machine order found", "未找到矿机订单");
        MESSAGE_I18N_MAP.put("machine not found", "矿机不存在");
        MESSAGE_I18N_MAP.put("machine is not on sale", "矿机未上架");
        MESSAGE_I18N_MAP.put("coin not found", "币种不存在");
        MESSAGE_I18N_MAP.put("coinSymbol is required", "币种不能为空");
        MESSAGE_I18N_MAP.put("quantity must be greater than 0", "数量必须大于0");
        MESSAGE_I18N_MAP.put("pCount must be greater than 0", "购买数量P必须大于0");
        MESSAGE_I18N_MAP.put("pay amount must be >= 0", "支付金额不能小于0");
        MESSAGE_I18N_MAP.put("please bind receive address before buying", "请先绑定收款地址后购买");
        MESSAGE_I18N_MAP.put("usdtPay + usdcPay must equal total amount", "USDT与USDC支付总额必须等于订单总额");
        MESSAGE_I18N_MAP.put("sys_config machine_price_per_p_usd must be greater than 0", "系统配置 machine_price_per_p_usd 必须大于0");

        MESSAGE_I18N_MAP.put("username is required", "用户名不能为空");
        MESSAGE_I18N_MAP.put("email is required", "邮箱不能为空");
        MESSAGE_I18N_MAP.put("account is required", "账号不能为空");
        MESSAGE_I18N_MAP.put("password is required", "密码不能为空");
        MESSAGE_I18N_MAP.put("registerInviteCode is required", "系统邀请码不能为空");
        MESSAGE_I18N_MAP.put("admin register invite code is not configured", "管理员注册邀请码未配置");
        MESSAGE_I18N_MAP.put("image is required", "图片不能为空");
        MESSAGE_I18N_MAP.put("oldPassword is required", "旧密码不能为空");
        MESSAGE_I18N_MAP.put("newPassword is required", "新密码不能为空");
        MESSAGE_I18N_MAP.put("newPassword must be at least 6 characters", "新密码长度不能少于6位");
        MESSAGE_I18N_MAP.put("password must be at least 6 characters", "密码长度不能少于6位");
        MESSAGE_I18N_MAP.put("invite code not found", "邀请码不存在");
        MESSAGE_I18N_MAP.put("username already exists", "用户名已存在");
        MESSAGE_I18N_MAP.put("email already exists", "邮箱已存在");
        MESSAGE_I18N_MAP.put("invalid token", "无效的登录令牌");
        MESSAGE_I18N_MAP.put("token expired", "登录已过期，请重新登录");
        MESSAGE_I18N_MAP.put("token is required", "登录令牌不能为空");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<String>> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if ("/".equals(uri)) {
            log.debug("404 root not found: {} {}", request.getMethod(), uri);
        } else {
            log.warn("404 not found: {} {}", request.getMethod(), uri);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new R<>(404, "资源不存在", null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<String>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("bad request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.badRequest(toZhMessage(e.getMessage())));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<R<String>> handleApiException(ApiException e) {
        log.warn("api exception: status={}, msg={}", e.getHttpStatus().value(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus())
                .body(R.fail(e.getCode(), toZhMessage(e.getMessage())));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            BindException.class
    })
    public ResponseEntity<R<String>> handleBadRequest(Exception e) {
        if (e instanceof MissingRequestHeaderException missingRequestHeaderException) {
            String headerName = missingRequestHeaderException.getHeaderName();
            if ("Authorization".equalsIgnoreCase(headerName)) {
                log.warn("unauthorized: missing Authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(R.unauthorized("请求头缺少Authorization"));
            }
        }
        String msg = "请求参数不合法";
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            msg = e.getMessage();
        }
        log.warn("bad request: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.badRequest(toZhMessage(msg)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("method not allowed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(HttpStatus.METHOD_NOT_ALLOWED, "请求方法不允许"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<R<String>> handleDataConflict(DataIntegrityViolationException e) {
        log.warn("data conflict: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(R.conflict("数据冲突"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<String>> handleException(Exception e) {
        log.error("Unhandled server exception: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.serverError("系统异常: " + toZhMessage(e.getMessage())));
    }

    private String toZhMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return "系统异常";
        }
        String result = msg;
        for (Map.Entry<String, String> entry : MESSAGE_I18N_MAP.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
