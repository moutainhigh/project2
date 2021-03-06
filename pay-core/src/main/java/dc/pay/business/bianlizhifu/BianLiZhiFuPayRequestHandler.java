package dc.pay.business.bianlizhifu;

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
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("BIANLIZHIFU")
public final class BianLiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BianLiZhiFuPayRequestHandler.class);

    private static final String    pay_memberid ="pay_memberid";           ///   商户号		必填	string	平台分配商户号
    private static final String    pay_orderid ="pay_orderid";           ///   订单编号		必填	String	订单号长度不超过50
    private static final String    pay_applydate ="pay_applydate";           ///   提交时间		必填	Date	时间格式：2017-07-25
    private static final String    pay_bankcode ="pay_bankcode";           ///   银行编号	  微信扫码（WXZF）
    private static final String    pay_notifyurl ="pay_notifyurl";           ///   服务端通知地址
    private static final String    pay_callbackurl ="pay_callbackurl";           ///   页面跳转通知地址
    private static final String    pay_amount ="pay_amount";           ///   商品金额		必填 商品金额（精确小数点后两位）
    private static final String    pay_tongdao ="pay_tongdao";           ///   通道名称	 微信扫码:Ywx  不参与签名
    private static final String      sign	   = "sign" ;                //是	string	签名


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
            payParam.put(pay_memberid,channelWrapper.getAPI_MEMBERID());
            payParam.put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(pay_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(pay_applydate, dc.pay.admin.core.util.DateUtil.getAllTime());
            payParam.put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
            payParam.put(pay_tongdao,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);

        log.debug("[便利支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()   )   || pay_tongdao.equalsIgnoreCase(paramKeys.get(i).toString())  )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString();//.replaceFirst("&key=","");
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[便利支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("successno") &&   jsonResultStr.containsKey("data")){
                        JSONObject jsonResultData =jsonResultStr.getJSONObject("data");
                            if(StringUtils.isNotBlank(jsonResultData.getString("pay_url"))){
                                if(HandlerUtil.isYLKJ(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper)){
                                    result.put(JUMPURL,  jsonResultData.getString("pay_url"));
                                }else{
                                    result.put(QRCONTEXT,  jsonResultData.getString("pay_url"));
                                }
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                 
            }
        } catch (Exception e) { 
             log.error("[便利支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[便利支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[便利支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}