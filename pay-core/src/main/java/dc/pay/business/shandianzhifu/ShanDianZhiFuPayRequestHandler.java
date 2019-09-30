package dc.pay.business.shandianzhifu;

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
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("SHANDIANZHIFU")
public final class ShanDianZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


    private static final  String version ="version";  //		varchar(5)		版本号    默认1.0
    private static final  String customerid ="customerid";  //		int(8)		商户编号    商户后台获取
    private static final  String sdorderno ="sdorderno";  //		varchar(20)商户订单号
    private static final  String total_fee ="total_fee";  //		decimal(10,订单金额    2)		精确到小数点后两位，例如10.24
    private static final  String paytype ="paytype";  //		varchar(10)		支付编号    详见附录1
    private static final  String bankcode ="bankcode";  //		varchar(10)	银行编号    网银直连不可为空，其他支付方式可为空	详见附录2
    private static final  String notifyurl ="notifyurl";  //		varchar(50)		异步通知URL    不能带有任何参数
    private static final  String returnurl ="returnurl";  //		varchar(50)		同步跳转URL    不能带有任何参数
    private static final  String remark ="remark";  //		varchar(50)	订单备注说明    Y	可为空
    private static final  String get_code ="get_code";  //		tinyint(1)	获取微信二维码    Y	如果只想获取被扫二维码，请设置get_code=1
    private static final  String sign ="sign";  //		varchar(32)		md5签名串    参照md5签名说明





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(version,"1.0");
            payParam.put(customerid,channelWrapper.getAPI_MEMBERID());
            payParam.put(sdorderno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
           // payParam.put(bankcode,null);
            payParam.put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(returnurl,channelWrapper.getAPI_WEB_URL() );
            payParam.put(remark,channelWrapper.getAPI_ORDER_ID() );
            if(HandlerUtil.isWEBWAPAPP_SM(channelWrapper)){ payParam.put(get_code, "1");}
        }

        log.debug("[闪电支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        //version={value}&customerid={value}&total_fee={value}&sdorderno={value}&notifyurl={value}&returnurl={value}&{apikey}
        String paramsStr = String.format("version=%s&customerid=%s&total_fee=%s&sdorderno=%s&notifyurl=%s&returnurl=%s&%s",
                params.get(version),
                params.get(customerid),
                params.get(total_fee),
                params.get(sdorderno),
                params.get(notifyurl),
                params.get(returnurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[闪电支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
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
				
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
				
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else if(StringUtils.isNotBlank(resultStr) ){
                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "1".equalsIgnoreCase(jsonResultStr.getString("status"))
                            && jsonResultStr.containsKey("payurl") && StringUtils.isNotBlank(jsonResultStr.getString("payurl"))){
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL, jsonResultStr.getString("payurl"));
                        }else{
                            result.put(QRCONTEXT, jsonResultStr.getString("payurl"));
                        }
                        payResultList.add(result);
                    }else {throw new PayException(resultStr); }
				}else{ throw new PayException(EMPTYRESPONSE);}
                 
            }
        } catch (Exception e) { 
             log.error("[闪电支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[闪电支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[闪电支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}