package dc.pay.business.huijuzhifu;

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

@RequestPayHandler("HUIJUZHIFU")
public final class HuiJuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiJuZhiFuPayRequestHandler.class);

    private static final String      p1_MerchantNo = "p1_MerchantNo";   // 商户编号
    private static final String      p2_OrderNo = "p2_OrderNo";   // 商户订单号
    private static final String      p3_Amount = "p3_Amount";   // 订单金额
    private static final String      p4_Cur = "p4_Cur";   // 交易币种   默认设置为1
    private static final String      p5_ProductName = "p5_ProductName";   // 商品名称
    private static final String      p7_ReturnUrl = "p7_ReturnUrl";   //  商户页面通知
    private static final String      p8_NotifyUrl = "p8_NotifyUrl";   // 服务器异步通
    private static final String      p9_FrpCode = "p9_FrpCode";   // 银行编码
    private static final String      pa_OrderPeriod = "pa_OrderPeriod";   //  建议填0


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(p1_MerchantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(p2_OrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(p3_Amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(p4_Cur,"1");
            payParam.put(p5_ProductName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(p7_ReturnUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(p8_NotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(p9_FrpCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(pa_OrderPeriod,"0");
        }

        log.debug("[汇聚支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        // opstate=opstate值&orderid=orderid值&ovalue=ovalue值&parter=商家接口调用ID&key=商家
        String paramsStr = String.format("%s%s%s%s%s%s%s%s%s%s",
                params.get(p1_MerchantNo),
                params.get(p2_OrderNo),
                params.get(p3_Amount),
                params.get(p4_Cur),
                params.get(p5_ProductName),
                params.get(p7_ReturnUrl),
                params.get(p8_NotifyUrl),
                params.get(p9_FrpCode),
                params.get(pa_OrderPeriod),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[汇聚支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
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
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
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
        } catch (Exception e) { 
             log.error("[汇聚支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[汇聚支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[汇聚支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}