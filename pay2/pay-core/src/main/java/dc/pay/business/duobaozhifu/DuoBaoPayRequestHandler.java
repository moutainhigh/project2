package dc.pay.business.duobaozhifu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

@RequestPayHandler("DUOBAOZHIFU")
public final class DuoBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DuoBaoPayRequestHandler.class);

    private static  final  String  parter = "parter";//   商户ID   否  商户id，由多宝商户系统分配
    private static  final  String  type = "type";//   银行类型   否  银行类型，具体请参考附录1
    private static  final  String  value = "value";//   金额   否    单位元（人民币） ，2位小数，最小支付金额    为1.00,微信支付宝至少2元
    private static  final  String  orderid = "orderid";//    商户订单号   否
    private static  final  String  callbackurl = "callbackurl";//  异步通知地址   否


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(parter,channelWrapper.getAPI_MEMBERID());
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(value,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }

        log.debug("[多宝支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // ={0}&={1}&={2}& ={3}&={4}key
        String paramsStr = String.format("parter=%s&type=%s&value=%s&orderid=%s&callbackurl=%s%s",
                params.get(parter),
                params.get(type),
                params.get(value),
                params.get(orderid),
                params.get(callbackurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[多宝支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
//        if ( HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
            payResultList.add(result);
        }
        
//        else{
//            payParam.put("qronly","QR");
//            HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
//            String qrContent=null;
//            if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
//                HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
//                if(payUrlInput!=null ){
//                    String qrContentSrc = payUrlInput.getValueAttribute();
//                    if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
//                }
//            }
//           if(StringUtils.isNotBlank(qrContent)){
//                result.put(QRCONTEXT, qrContent);
//                payResultList.add(result);
//            }else {  throw new PayException(endHtml.asXml()); }
//        }
    
        log.debug("[多宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[多宝支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}