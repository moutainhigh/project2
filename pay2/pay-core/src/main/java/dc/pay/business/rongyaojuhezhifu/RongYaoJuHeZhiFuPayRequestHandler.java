package dc.pay.business.rongyaojuhezhifu;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * @author Cobby
 * July 18, 2019
 */
@RequestPayHandler("RONGYAOJUHEZHIFU")
public final class RongYaoJuHeZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RongYaoJuHeZhiFuPayRequestHandler.class);

    private static final String pay_memberid    = "pay_memberid";     // 平台分配商户号
    private static final String pay_orderid     = "pay_orderid";      // 上送订单号唯一
    private static final String pay_applydate   = "pay_applydate";    // 时间格式：2016-12-26 18:18:18
    private static final String pay_bankcode    = "pay_bankcode";     // 参考后续说明
    private static final String pay_notifyurl   = "pay_notifyurl";    // 服务端返回地址
    private static final String pay_callbackurl = "pay_callbackurl";  // 页面跳转返回地址
    private static final String pay_amount      = "pay_amount";       // 商品金额
    private static final String pay_productname = "pay_productname";  // 商品名称    是    否
    private static final String pay_version     = "pay_version";  // 系统接口版本 是 否 固定值:vb1.0

    private static final String key = "key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid, channelWrapper.getAPI_ORDER_ID());
                put(pay_applydate, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(pay_bankcode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_notifyurl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl, channelWrapper.getAPI_WEB_URL());
                put(pay_amount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_productname, channelWrapper.getAPI_ORDER_ID());
                put(pay_version, "2.0");
            }
        };
        log.debug("[荣耀聚合]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!pay_version.equals(paramKeys.get(i)) && !pay_productname.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key + "=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[荣耀聚合]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString().replace("method='post'", "method='post'"));
        } catch (Exception e) {
            log.error("[荣耀聚合]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[荣耀聚合]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[荣耀聚合]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}