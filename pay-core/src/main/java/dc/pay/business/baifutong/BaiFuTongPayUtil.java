package dc.pay.business.baifutong;/**
 * Created by admin on 2017/6/8.
 */

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class BaiFuTongPayUtil {

    public enum ServerErrorMsg {
        E0("0", "成功"),
        E10001("10001", "请求参数异常"),
        E10002("10002", "响应数据异常"),
        E10003("10003", "JSON格式异常"),
        E10004("10004", "请求参数签名异常"),
        E10005("10005", "系统忙,请稍后再试"),
        E10006("10006", "系统维护,暂停服务"),
        E10007("10007", "通道维护,暂停服务"),
        E10010("10010", "当前商户请求过于频繁"),
        E10011("10011", "当前IP请求过于频繁"),
        E20001("20001", "商户信息不存在"),
        E20002("20002", "商户信息不可用"),
        E20003("20003", "支付通道不存在"),
        E20004("20004", "支付通道不可用"),
        E20005("20005", "商户关联配置错误"),
        E20006("20006", "商户关联配置错误"),
        E20007("20007", "商户订单号重复"),
        E20008("20008", "订单处理异常"),
        E20009("20009", "订单处理异常"),
        E20016("20016", "订单状态异常"),
        E20017("20017", "商户关联配置错误"),
        E20018("20018", "商户关联配置错误"),
        E20019("20019", "商户关联配置错误"),
        E20020("20020", "商户关联配置错误"),
        E20021("20021", "商户关联配置错误"),
        E20022("20022", "商户关联配置错误"),
        E20023("20023", "商户关联配置错误"),
        E20024("20024", "商户关联配置错误"),
        E20025("20025", "商户关联配置错误"),
        E20026("20026", "商户关联配置错误"),
        E20027("20027", "商户关联配置错误"),
        E20101("20101", "接口请求返回值错误"),
        E20102("20102", "回调报文订单号无效"),
        E20103("20103", "订单关联配置错误"),
        E20104("20104", "订单状态更新异常"),
        E20110("20110", "订单状态更新异常"),
        E20111("20111", "支付金额与订单金额不符"),
        E20112("20112", "订单号不存在"),
        E30001("30001", "支付通道初始化异常"),
        E30002("30002", "接口请求异常"),
        E30003("30003", "接口请求异常"),
        E30004("30004", "接口请求异常"),
        E30005("30005", "接口请求异常"),
        E30006("30006", "接口请求异常"),
        E30007("30007", "接口请求异常"),
        E30020("30020", "支付通道未实现"),
        E30021("30021", "商户订单号重复"),
        E30201("30201", "获取微信OPENID异常"),
        E30202("30202", "获取订单缓存异常"),
        E30203("30203", "支付接口返回错误"),
        E99999("99999", "未知异常");
        String code;
        String msg;
        ServerErrorMsg(String code, String msg) {
            this.code = code;
            this.msg = msg;
        }
        public String getCode() {
            return code;
        }
        public String getMsg() {
            return msg;
        }
        public static String getMsgByCode(String code) {
            String msg = "无此错误代码：" + code;
            for (BaiFuTongPayUtil.ServerErrorMsg c : BaiFuTongPayUtil.ServerErrorMsg.values()) {
                if (c.getCode().equalsIgnoreCase(code)) {
                    return code + ":" + c.getMsg();
                }
            }
            return msg;
        }
    }
}
