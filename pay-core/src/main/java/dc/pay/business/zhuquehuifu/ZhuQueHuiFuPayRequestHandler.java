package dc.pay.business.zhuquehuifu;

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

@RequestPayHandler("ZHUQUEHUIFU")
public final class ZhuQueHuiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ZhuQueHuiFuPayRequestHandler.class);

     private static final String      money = "money"; 	    //long	是		订单金额,单位分
     private static final String      return_url = "return_url"; 	    //String	是		用户支付完成跳转的页面
     private static final String      group_id = "group_id"; 	    //String	是		接口使用者ID 由系统开发商提供
     private static final String      notify_url = "notify_url"; 	    //String	是		用于接收支付成功消息的地址
     private static final String      pay_code = "pay_code"; 	    //String	否		支付方式 默认值alipay 暂无其他可选值
     private static final String      user_order_sn = "user_order_sn"; 	    //String	是		订单号 本系统不提供订单号重复判断 需接口使用者自行判断
     private static final String      subject = "subject"; 	    //String	是		订单标题
     private static final String      pay_from = "pay_from"; 	    //String	否		电脑网站支付还是手机网页支付;值为WEB或WAP选其一;不传默认为WAP
     private static final String      sign = "sign"; 	    //String	是		签名sign=md5(group_id的值+user_order_sn的值+money的值+return_url的值+商户秘钥),注意顺序




    private static final String      request_data	   = "request_data" ;
    private static final String      WAP	   = "WAP" ;




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(money,channelWrapper.getAPI_AMOUNT());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(group_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(pay_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(user_order_sn,channelWrapper.getAPI_ORDER_ID());
            payParam.put(subject,  channelWrapper.getAPI_ORDER_ID() );
            payParam.put(pay_from, WAP  );
        }

        log.debug("[朱雀慧付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // =md5(group_id的值+user_order_sn的值+money的值+return_url的值+商户秘钥)
        String paramsStr = String.format("%s%s%s%s%s",
                params.get(group_id),
                params.get(user_order_sn),
                params.get(money),
                params.get(return_url),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[朱雀慧付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        String requestJsonData = JSON.toJSONString(payParam);
        Map<String, String> payJsonParam = Maps.newHashMap();
        payJsonParam.put(request_data,HandlerUtil.UrlEncode(requestJsonData));


        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payJsonParam).toString().replace("method='post'","method='get'"));
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
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payJsonParam, String.class, HttpMethod.GET);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
				
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "1".equalsIgnoreCase(jsonResultStr.getString("status"))
                            && jsonResultStr.containsKey("payurl") && StringUtils.isNotBlank(jsonResultStr.getString("payurl"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString("payurl"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString("payurl"));
                        }
                        payResultList.add(result);
                    }else if(StringUtils.isBlank(resultStr) ){  throw new PayException(EMPTYRESPONSE);  }else {throw new PayException(resultStr); }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[朱雀慧付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[朱雀慧付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[朱雀慧付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}