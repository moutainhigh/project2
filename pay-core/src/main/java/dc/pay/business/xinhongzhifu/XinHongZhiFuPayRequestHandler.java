package dc.pay.business.xinhongzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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

/**
 * @author Cobby
 * Jan 29, 2019
 */
@RequestPayHandler("XINHONGZHIFU")
public final class XinHongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinHongZhiFuPayRequestHandler.class);

    private static final String pay_memberid                ="pay_memberid";    //pay_memberid	商户号	int	Y	Y	10007
    private static final String pay_orderid                 ="pay_orderid";     //pay_orderid	商户订单号	string	Y	Y	20180413767827897
    private static final String pay_applydate               ="pay_applydate";   //pay_applydate	订单提交时间	DateTime	Y	Y	2018-04-13 23:24:19
    private static final String pay_returnType              ="pay_returnType";  //pay_returnType	返回类型	string	Y	N	返回类型 如传 json 将返回json 内容、传http 页面将直接跳转到收银台默认json
    private static final String pay_bankcode                ="pay_bankcode";    //pay_bankcode	支付方式	int	Y	Y	902=微信扫码 914=微信H5 907=网银支付 903=支付宝扫码 904=支付宝WAP 912=快捷支付 915=银联WAP 916=银联扫码
    private static final String pay_notifyurl               ="pay_notifyurl";   //pay_notifyurl	异步通知地址	string	Y	Y	http://www/qq/com
    private static final String pay_callbackurl             ="pay_callbackurl"; //pay_callbackurl	页面同步通知地址	string	Y	Y	http://www/qq/com
    private static final String pay_amount                  ="pay_amount";      //pay_amount	订单金额	float	Y	Y	单位：元
    private static final String pay_productname             ="pay_productname"; //pay_productname	商品名称	string	Y	N	iphone6
    private static final String clientip                    ="clientip";        //clientip	客户真实ip	string	Y	N	127.0.0.1
    private static final String pay_md5sign                 ="pay_md5sign";     //pay_md5sign	签名	string	Y	N	签名方式
    private static final String key                         ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_applydate,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(pay_returnType,"json");
                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_productname,  "name");
                put(clientip,  channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[信宏支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!pay_productname.equalsIgnoreCase(paramKeys.get(i).toString())&& !pay_returnType.equalsIgnoreCase(paramKeys.get(i).toString()) && !clientip.equalsIgnoreCase(paramKeys.get(i).toString()) &&
		            StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key + "="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[信宏支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
	    try {
		    String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST,defaultHeaders);
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[信宏支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("status") && "0000".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("pay_info") && StringUtils.isNotBlank(jsonObject.getString("pay_info"))) {
                    String code_url = jsonObject.getString("pay_info").replaceAll("amp;","");
                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                }else {
                    log.error("[信宏支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            
        } catch (Exception e) {
            log.error("[信宏支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[信宏支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[信宏支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}