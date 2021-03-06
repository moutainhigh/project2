package dc.pay.business.mtzhifu3;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("MTZHIFU3")
public final class MTZhiFu3PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MTZhiFu3PayRequestHandler.class);

     private static final String  sign = "sign";    //	签名	String		是	规则详见sign签名规则
     private static final String  merchantCode = "merchantCode";    //	商户号	String		是	商户号可登陆mt-支付系统获取
     private static final String  merchantTransactionId = "merchantTransactionId";    //	订单ID	String		是	商户自定义
     private static final String  amount = "amount";    //	订单金额	Number		是	金额精确到分,保留两位小数
     private static final String  platform = "platform";    //	支付平台	String		是	详见移动平台(MobilePlatform)
     private static final String  type = "type";    //	请求类型	String		是	详见移动支付请求类型(MobilePayReqType)
     private static final String  notifyUrl = "notifyUrl";    //	通知回调	String		是
     private static final String  token = "token";

     private static final String  successful = "successful";
     private static final String  data = "data";
     private static final String  trueStr = "true";


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merchantCode,channelWrapper.getAPI_MEMBERID());
            payParam.put(merchantTransactionId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(platform,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[MT支付3]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()) ||token.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("token=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[MT支付3]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
               // System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey(successful) && trueStr.equalsIgnoreCase(jsonResultStr.getString(successful)) && jsonResultStr.containsKey(data)
                            && null!=jsonResultStr.getJSONObject(data)  && jsonResultStr.getJSONObject(data).containsKey(data) ){
                            if(StringUtils.isNotBlank( jsonResultStr.getJSONObject(data).getString(data) )){
                                result.put(JUMPURL, jsonResultStr.getJSONObject(data).getString(data) );
                                payResultList.add(result);
                            }else{ throw new PayException(resultStr);}
                    }else {
                        throw new PayException(resultStr);
                    }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[MT支付3]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[MT支付3]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[MT支付3]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}