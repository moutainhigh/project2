package dc.pay.business.rongbaozhifu;

/**
 * ************************
 * @author tony 3556239829
 */

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
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("RONGBAOZHIFU")
public final class RongBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RongBaoZhiFuPayRequestHandler.class);

     private static final String    partner="partner";  //     ID
     private static final String    banktype="banktype";  //     类型
     private static final String    paymoney="paymoney";  //     金额
     private static final String    ordernumber="ordernumber";  //     商户订单号
     private static final String    callbackurl="callbackurl";  //     下行异步通知地址
     private static final String    hrefbackurl="hrefbackurl";  //     下行同步通知地址
     private static final String    sign="sign";  //     MD5签名
     private static final String    bankcode  ="bankcode";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(partner,channelWrapper.getAPI_MEMBERID());
            payParam.put(banktype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());

            if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("WEBWAPAPP_WX_SM") || HandlerUtil.isYLKJ(channelWrapper)){
                payParam.put(paymoney,HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT())); //微信没有0
            }else{
                payParam.put(paymoney,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));  //其他的有0
            }
            if(HandlerUtil.isYLKJ(channelWrapper)){
                payParam.put(bankcode,bankcode);
            }
            payParam.put(ordernumber,channelWrapper.getAPI_ORDER_ID());
            payParam.put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(hrefbackurl,channelWrapper.getAPI_WEB_URL());
        }
        log.debug("[融宝支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //partner={}&banktype={}&paymoney={}&ordernumber={}&callbackurl={}key

        String paramsStr = String.format("partner=%s&banktype=%s&paymoney=%s&ordernumber=%s&callbackurl=%s%s",
                params.get(partner),
                params.get(banktype),
                params.get(paymoney),
                params.get(ordernumber),
                params.get(callbackurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[融宝支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
/*				
				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
                String qrContent=null;
                if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
                    HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
                    if(payUrlInput!=null ){
                        String qrContentSrc = payUrlInput.getValueAttribute();
                        if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
                    }
                }
               if(StringUtils.isNotBlank(qrContent)){
                    result.put(QRCONTEXT, qrContent);
                    payResultList.add(result);
                }else {  throw new PayException(endHtml.asXml()); }
				
*/				
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{


                    if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("WEBWAPAPP_WX_SM")){ //微信扫码
                        result.put(QRCONTEXT, HandlerUtil.UrlDecode(resultStr));
                        payResultList.add(result);
                    }else{
                        JSONObject jsonResultStr = JSON.parseObject(resultStr);
                        if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeimg")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeimg"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeimg")));
                                payResultList.add(result);
                            }
                        }else {
                            throw new PayException(resultStr);
                        }

                    }
				




				}
                 
            }
        } catch (Exception e) { 
             log.error("[融宝支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[融宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[融宝支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}