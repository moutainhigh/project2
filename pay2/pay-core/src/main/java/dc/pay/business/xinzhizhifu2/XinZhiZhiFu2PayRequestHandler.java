package dc.pay.business.xinzhizhifu2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 26, 2019
 */
@RequestPayHandler("XINZHIZHIFU2")
public final class XinZhiZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinZhiZhiFu2PayRequestHandler.class);

    //参数名称    参数含义    是否必填    参与签名    参数说明
    //pay_memberid    商户号 是   是   平台分配商户号
    //pay_orderid     订单号 是   是   上送订单号唯一, 字符长度20
    //pay_applydate   提交时间    是   是   时间格式：2016-12-26 18:18:18
    //pay_bankcode    银行编码    是   是   参考后续说明
    //pay_notifyurl   服务端通知   是   是   服务端返回地址.（POST返回数据）
    //pay_callbackurl 页面跳转通知  是   是   页面跳转返回地址（POST返回数据）
    //pay_amount      订单金额    是   是   商品金额
    //pay_md5sign     MD5签名   是   否   请看MD5签名字段格式
    //pay_attach      附加字段    否   否   此字段在返回时按原样返回 (中文需要url编码)
    //pay_productname 商品名称    是   否   
    private static final String pay_memberid                ="pay_memberid";
    private static final String pay_orderid                 ="pay_orderid";
    private static final String pay_amount                  ="pay_amount";
    private static final String pay_applydate               ="pay_applydate";
    private static final String pay_bankcode                ="pay_bankcode";
    private static final String pay_notifyurl               ="pay_notifyurl";
    private static final String pay_callbackurl             ="pay_callbackurl";
    private static final String pay_productname             ="pay_productname";
//    private static final String pay_md5sign                 ="pay_md5sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_applydate, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
                put(pay_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_productname,handlerUtil.getRandomStr(5));
            }
        };
        log.debug("[新智支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!pay_productname.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新智支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
//            result.put(HTMLCONTEXT, getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
////        String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
////        String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[新智支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//                log.error("[新智支付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            JSONObject jsonObject = null;
//            try {
//                jsonObject = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                log.error("[新智支付2]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
//                //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(e.getMessage(),e);
//            }          
//            //只取正确的值，其他情况抛出异常
//            if (null != jsonObject && jsonObject.containsKey("status") && "success".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
//                JSONObject jsonObject2 = jsonObject.getJSONObject("data");
//                if (null != jsonObject2 && jsonObject2.containsKey("url") && StringUtils.isNotBlank(jsonObject2.getString("url"))) {
//                    if (handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebWyKjzf(channelWrapper)) {
//                        result.put(JUMPURL, jsonObject2.getString("url"));
//                    }else {
//                        String qr = QRCodeUtil.decodeByUrl(jsonObject2.getString("url"));
//                        if (StringUtils.isBlank(qr)) {
//                            log.error("[新智支付2]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                            throw new PayException(resultStr);
//                        }
//                        result.put(QRCONTEXT, qr);
//                    }
//                }else {
//                    log.error("[新智支付2]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//            }else {
//                log.error("[新智支付2]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新智支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新智支付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    public static final synchronized StringBuffer getHtmlContent(String actionUrl,Map<String, ?> payParam){
        StringBuffer sbHtml = new StringBuffer();
        sbHtml.append("<header>");
        sbHtml.append("<meta http-equiv='expires' content='0'>");
        sbHtml.append("<meta http-equiv='pragma' content='no-cache'>");
        sbHtml.append("<meta http-equiv='cache-control' content='no-cache'>");
        sbHtml.append("<meta name='viewport' content='width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0'>");
        sbHtml.append("<meta name='format-detection' content='telephone=no'>");
        sbHtml.append("<meta name='apple-mobile-web-app-capable' content='yes'>");
        sbHtml.append("<meta name='referrer' content='never'>");
        sbHtml.append("</header>");
        sbHtml.append("<input type='hidden' id='timestamp' value='"+System.currentTimeMillis()+"'/>");
        sbHtml.append("<div id='msg' style='padding:8px;border:1px solid #96c2f1;background:#eff7ff;text-align: center;'> 正在支付...... <div>");
        sbHtml.append("<form id='payform' name='payform' action='" + actionUrl + "' method='post'>");
        for (Map.Entry<String, ?> entry : payParam.entrySet()) {
            sbHtml.append("<input type='hidden' name='" + entry.getKey() + "' value='" + entry.getValue() + "'/>");
        }
        sbHtml.append("</form>");
        sbHtml.append("<script>var timestampC=document.getElementById('timestamp').value; var timestampB=new Date().getTime();if(timestampB-timestampC>86400000){  document.getElementById('msg').innerHTML='下单时间过长，为避免重复支付，请先退出浏览器后，重新下单。';}else{document.forms['payform'].submit();}</script>");
       return sbHtml;
    }
}