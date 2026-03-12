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
        MESSAGE_I18N_MAP.put("Invalid request", "请求参数不合法");
        MESSAGE_I18N_MAP.put("Method not allowed", "请求方法不允许");
        MESSAGE_I18N_MAP.put("Data conflict", "数据冲突");
        MESSAGE_I18N_MAP.put("System error", "系统异常");

        MESSAGE_I18N_MAP.put("请求体不能为空", "请求参数不能为空");
        MESSAGE_I18N_MAP.put("文件不能为空", "请上传文件");
        MESSAGE_I18N_MAP.put("用户编号不能为空", "用户编号不能为空");
        MESSAGE_I18N_MAP.put("编号不能为空", "编号不能为空");
        MESSAGE_I18N_MAP.put("矿机编号不能为空", "矿机编号不能为空");
        MESSAGE_I18N_MAP.put("名称不能为空", "名称不能为空");
        MESSAGE_I18N_MAP.put("算力值必须大于0", "算力数值必须大于0");
        MESSAGE_I18N_MAP.put("算力单位不能为空", "算力单位不能为空");
        MESSAGE_I18N_MAP.put("不支持的算力单位：", "不支持的算力单位：");
        MESSAGE_I18N_MAP.put("单价必须大于0", "单价必须大于0");
        MESSAGE_I18N_MAP.put("锁定天数必须大于等于0", "锁仓天数不能小于0");
        MESSAGE_I18N_MAP.put("算力值不能为空", "算力数值不能为空");
        MESSAGE_I18N_MAP.put("金额必须大于0", "金额必须大于0");
        MESSAGE_I18N_MAP.put("余额不足", "余额不足");
        MESSAGE_I18N_MAP.put("收款地址不能为空", "收款地址不能为空");
        MESSAGE_I18N_MAP.put("收款地址已存在", "收款地址已存在");
        MESSAGE_I18N_MAP.put("收款地址不存在", "收款地址不存在");
        MESSAGE_I18N_MAP.put("提现前请先绑定收款地址", "请先绑定收款地址后再提现");
        MESSAGE_I18N_MAP.put("资金密码不能为空", "资金密码不能为空");
        MESSAGE_I18N_MAP.put("未设置资金密码，请先在设置中设置", "未设置资金密码，请先在设置中配置");
        MESSAGE_I18N_MAP.put("旧资金密码错误", "旧资金密码错误");
        MESSAGE_I18N_MAP.put("资金密码错误", "资金密码错误");
        MESSAGE_I18N_MAP.put("用户不存在", "用户不存在");
        MESSAGE_I18N_MAP.put("网络不能为空", "网络不能为空");
        MESSAGE_I18N_MAP.put("网络必须是 TRC20/ERC20/BTC 之一", "网络必须为TRC20/ERC20/BTC");
        MESSAGE_I18N_MAP.put("币种与网络不匹配", "资产与网络不匹配");
        MESSAGE_I18N_MAP.put("币种和网络不能为空", "资产和网络不能为空");
        MESSAGE_I18N_MAP.put("币种不能为空", "资产不能为空");
        MESSAGE_I18N_MAP.put("币种必须是 USDT/USDC/BTC 之一", "资产必须为USDT/USDC/BTC");
        MESSAGE_I18N_MAP.put("状态不能为空", "状态不能为空");
        MESSAGE_I18N_MAP.put("状态必须是 1(通过) 或 2(拒绝)", "状态必须为1(通过)或2(拒绝)");
        MESSAGE_I18N_MAP.put("状态必须是0或1", "状态只能为0或1");
        MESSAGE_I18N_MAP.put("配置键不能为空", "配置键不能为空");
        MESSAGE_I18N_MAP.put("配置不存在", "配置不存在");
        MESSAGE_I18N_MAP.put("充值地址配置仅可在数据库中修改", "收款地址配置仅允许在数据库中修改");

        MESSAGE_I18N_MAP.put("订单不存在", "订单不存在");
        MESSAGE_I18N_MAP.put("该订单不属于当前用户", "该订单不属于当前用户");
        MESSAGE_I18N_MAP.put("订单不在持有状态", "订单不是持有中状态");
        MESSAGE_I18N_MAP.put("已有收益的订单不能取消", "已有收益的订单不能取消");
        MESSAGE_I18N_MAP.put("暂无可提现收益", "暂无可提现收益");
        MESSAGE_I18N_MAP.put("未找到算力订单", "未找到算力订单");
        MESSAGE_I18N_MAP.put("矿机不存在", "矿机不存在");
        MESSAGE_I18N_MAP.put("矿机未上架", "矿机未上架");
        MESSAGE_I18N_MAP.put("币种不存在", "币种不存在");
        MESSAGE_I18N_MAP.put("币种标识不能为空", "币种不能为空");
        MESSAGE_I18N_MAP.put("数量必须大于0", "数量必须大于0");
        MESSAGE_I18N_MAP.put("P数量必须大于0", "购买数量P必须大于0");
        MESSAGE_I18N_MAP.put("支付金额必须大于等于0", "支付金额不能小于0");
        MESSAGE_I18N_MAP.put("购买前请先绑定收款地址", "请先绑定收款地址后购买");
        MESSAGE_I18N_MAP.put("USDT支付与USDC支付之和必须等于总金额", "USDT与USDC支付总额必须等于订单总额");
        MESSAGE_I18N_MAP.put("系统配置 machine_price_per_p_usd 必须大于0", "系统配置 machine_price_per_p_usd 必须大于0");

        MESSAGE_I18N_MAP.put("用户名不能为空", "用户名不能为空");
        MESSAGE_I18N_MAP.put("邮箱不能为空", "邮箱不能为空");
        MESSAGE_I18N_MAP.put("账号不能为空", "账号不能为空");
        MESSAGE_I18N_MAP.put("密码不能为空", "密码不能为空");
        MESSAGE_I18N_MAP.put("注册邀请码不能为空", "系统邀请码不能为空");
        MESSAGE_I18N_MAP.put("未配置管理员注册邀请码", "管理员注册邀请码未配置");
        MESSAGE_I18N_MAP.put("图片不能为空", "图片不能为空");
        MESSAGE_I18N_MAP.put("旧密码不能为空", "旧密码不能为空");
        MESSAGE_I18N_MAP.put("新密码不能为空", "新密码不能为空");
        MESSAGE_I18N_MAP.put("新密码长度不能少于6位", "新密码长度不能少于6位");
        MESSAGE_I18N_MAP.put("密码长度不能少于6位", "密码长度不能少于6位");
        MESSAGE_I18N_MAP.put("邀请码不存在", "邀请码不存在");

        MESSAGE_I18N_MAP.put("username already exists", "用户名已存在");
        MESSAGE_I18N_MAP.put("email already exists", "邮箱已存在");
        MESSAGE_I18N_MAP.put("token expired", "登录已过期，请重新登录");
        MESSAGE_I18N_MAP.put("令牌不能为空", "登录令牌不能为空");
        MESSAGE_I18N_MAP.put("请求头缺少Authorization", "请求头缺少授权信息");
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
                        .body(R.unauthorized("请求头缺少授权信息"));
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
