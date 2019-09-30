package dc.pay.business.yingtongbao;/**
 * Created by admin on 2017/6/21.
 */

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class YingTongBaoPayUtil {
    public enum   ServerErrorMsg{
        E00("00", "交易成功"),
        E01("01", "系统错误"),
        E02("02", "参数有误:未上送支付方式"),
        E03("03", "找不到商户信息"),
        E04("04", "找不到机构编码"),
        E06("06", "系统异常"),
        E07("07", "原交易不属于公众号交易,其他错误"),
        E10("10", "正在支付中,需要发起查询交易"),
        E20("20", "通道方返回的交易异常"),
        E30("30", "通道方返回的格式有误"),
        E56("56", "通道方路由编码有误"),
        E57("57", "通道方未开通该业务"),
        E58("58", "找不到通道方路由"),
        E61("61", "该商户属于黑名单商户"),
        E62("62", "交易金额过高/低"),
        E63("63", "超过日限额"),
        E64("64", "超过月限额"),
        E94("94", "流水号重复"),
        E97("97", "找不到渠道商户"),
        E98("98", "渠道连接超时"),
        EA0("A0", "签名有误");
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
            String msg = "无此错误代码："+code;
            for (ServerErrorMsg c : ServerErrorMsg.values()) {
                if (c.getCode().equalsIgnoreCase(code)) {
                    return code+":"+c.getMsg();
                }
            }
            return msg;
        }
    }
}
