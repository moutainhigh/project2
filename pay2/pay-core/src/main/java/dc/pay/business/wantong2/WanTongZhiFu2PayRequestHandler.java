package dc.pay.business.wantong2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;


/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 06, 2019
 */
@RequestPayHandler("WANTONG2")
public final class WanTongZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WanTongZhiFu2PayRequestHandler.class);

    private static final String merchant_code           ="merchant_code"; // 商户编号
    private static final String appno_no                ="appno_no";      // 应用编号
    private static final String order_no                ="order_no";      // 商户订单号
    private static final String order_amount            ="order_amount";  // 交易金额
    private static final String order_time              ="order_time";    // 交易时间
    private static final String product_code            ="product_code";  // 商品编号
    private static final String product_name            ="product_name";  // 商品名称
    private static final String user_no                 ="user_no";       // 用户编号
    private static final String notify_url              ="notify_url";    // 异步通知地址
    private static final String pay_type                ="pay_type";      // 支付类型
    private static final String bank_code               ="bank_code";     // 如果选择网关支付此字段必填，各银行编码详见附录1
    private static final String return_url              ="return_url";    // 如果是网关，快捷支付，微信H5, 支付宝，此字段为必填字段。
    private static final String transdata               ="transdata";     //
    private static final String merchant_ip             ="merchant_ip";   //  IP  微信H5，此字段为必填字段

    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == api_MEMBERID || !api_MEMBERID.contains("&") || api_MEMBERID.split("&").length != 2) {
            log.error("[万通2]-[请求支付]-“支付通道商号”输入数据格式为：商户号&appid" );
            throw new PayException("[万通2]-[请求支付]-“支付通道商号”输入数据格式为：商户号&appid" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                if (handlerUtil.isWY(channelWrapper) && !handlerUtil.isWebYlKjzf(channelWrapper)) {
                    put(pay_type,"wangguan");
                    put(bank_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else {
                    put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(user_no,HandlerUtil.getRandomNumber(2,250)+"");
                put(product_name,"name");
                put(product_code,handlerUtil.getRandomStr(5));
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(order_time,HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMdd"));
                put(order_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(merchant_code, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(appno_no, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                if (channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().equalsIgnoreCase("weixin-h5")){
                    put(merchant_ip ,channelWrapper.getAPI_Client_IP());
            }
            }
        };
        log.debug("[万通2]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map params) throws PayException {
        String signMd5="";
        try {
            signMd5 = RsaUtil.signByPrivateKey(JSON.toJSONString(params), channelWrapper.getAPI_KEY(), "SHA256withRSA");    // 签名
        } catch (Exception e) {
            log.error("[万通2]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[万通2]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        Map<String, String> map = new TreeMap<String, String>() {
            {
                try {
                    put(transdata, URLEncoder.encode(JSON.toJSONString(payParam), "utf-8"));
                    put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), URLEncoder.encode(pay_md5sign, "utf-8"));
                    put("signtype", "signtype");
                } catch (UnsupportedEncodingException e) {
                    log.error("[万通2]-[请求支付]-3.0.发送支付请求，参数拼接出错：{}",e.getMessage(),e);
                    throw new PayException(e.getMessage(),e);
                }
            }
        };
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(),JSON.toJSONString(map),MediaType.APPLICATION_JSON_UTF8_VALUE,"Keep-Alive",MediaType.APPLICATION_JSON_UTF8_VALUE.toString());
        if (StringUtils.isBlank(resultStr)) {
            log.error("[万通2]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
            throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
        }
        JSONObject resJson = JSONObject.parseObject(resultStr);
        if (!resJson.containsKey("payment") || !"true".equals(resJson.getString("payment"))) {
            log.error("[万通2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        if (handlerUtil.isWebYlKjzf(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper)) {
            result.put(JUMPURL, resJson.getString("payUrl"));
        }else if (handlerUtil.isWY(channelWrapper)) {
            result.put(HTMLCONTEXT, resJson.getString("html"));
        }else {
            result.put(QRCONTEXT, resJson.getString("payUrl"));
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[万通2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[万通2]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}