package dc.pay.business.zaixianbao;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestPayHandler("ZAIXIANBAO")
public final class ZaiXianBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log =  LoggerFactory.getLogger(ZaiXianBaoPayRequestHandler.class);
    private static  final String  MERCHNO	 = "merchno";
    private static  final String  AMOUNT	 = "amount";
    private static  final String  TRACENO	 = "traceno";
    private static  final String  PAYTYPE	 = "payType";
    private static  final String  NOTIFYURL	 = "notifyUrl";
    private static  final String  GOODSNAME	 = "goodsName";
    private static  final String  SIGNATURE  = "signature";

    private static  final String   RESPCODE  = "respCode";
    private static  final String   MESSAGE  = "message";
    private static  final String   BARCODE	="barCode";
    private static  final String   QRCONTEXT = "QrContext";
    private static  final String HTMLCONTEXT = "HtmlContext";
    private static  final String PARSEHTML = "parseHtml";
    private static final String JUMPURL = "JUMPURL";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
            Map<String, String> payParam = Maps.newTreeMap();
            payParam.put(MERCHNO, channelWrapper.getAPI_MEMBERID());
            payParam.put(AMOUNT, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(TRACENO, channelWrapper.getAPI_ORDER_ID());
            payParam.put(PAYTYPE, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(GOODSNAME,"PAY");
            payParam.put(NOTIFYURL, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            log.debug("[在线宝]-[请求支付]-1.组装请求参数完成：{}",JSON.toJSONString(payParam));
            return payParam;
    }

    protected String buildPaySign(Map payParam) throws PayException {
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append(channelWrapper.getAPI_KEY());//"key="+
            pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString()).toLowerCase();
           log.debug("[在线宝]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(pay_md5sign));
           return pay_md5sign;
    }

    protected List<Map<String,String>> sendRequestGetResult(Map<String, String> payParam,String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String,String>> payResultList = Lists.newArrayList();
        try {
            String channel_flag = channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG();
            String channel_bank = channelWrapper.getAPI_CHANNEL_BANK_NAME();
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,String.class,HttpMethod.POST).trim();
            resultStr = new String(resultStr.getBytes("ISO-8859-1"), "GBK");
             JSONObject responseJsonObject = JSONObject.parseObject(resultStr);
              String respCode  = responseJsonObject.getString(RESPCODE) ;
              String message   = responseJsonObject.getString(MESSAGE) ;
              String merchno   = responseJsonObject.getString(MERCHNO) ;
              String traceno   = responseJsonObject.getString(TRACENO) ;
              String barCode	  = responseJsonObject.getString(BARCODE) ;
/*
            2017.11.13更新,第三方要求生成此网址二维码，并且要求国内IP才能扫码
              if("00".equalsIgnoreCase(respCode) && StringUtils.isNotBlank(barCode) && barCode.contains("?url=")){
                if(channel_bank.endsWith("WX_SM")  || channel_bank.endsWith("ZFB_SM") ||channel_bank.endsWith("BD_QB")  || channel_bank.endsWith("QQ_SM") || channel_bank.endsWith("JD_QB")){
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(QRCONTEXT,barCode.split("url=")[1]);
                    result.put(PARSEHTML,resultStr);
                    payResultList.add(result);
                }
            }

*/

              if("00".equalsIgnoreCase(respCode) && StringUtils.isNotBlank(barCode)){
                  if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_")||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAPAPP_")) {
                      if(StringUtils.isNotBlank(barCode)){
                          HashMap<String, String> result = Maps.newHashMap();
                          result.put(JUMPURL, barCode);
                          result.put(PARSEHTML,resultStr);
                          payResultList.add(result);
                      }else{
                          log.error("发送支付请求，及获取支付请求结果错误："+resultStr);
                          throw new PayException(resultStr );
                      }
                  }else  if(channel_bank.endsWith("WX_SM")  || channel_bank.endsWith("ZFB_SM") ||channel_bank.endsWith("BD_QB")  || channel_bank.endsWith("QQ_SM") || channel_bank.endsWith("JD_QB") || channel_bank.endsWith("YL_SM")){
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(QRCONTEXT,barCode);
                    result.put(PARSEHTML,resultStr);
                    payResultList.add(result);
                }

            } else{
                throw new PayException(resultStr);
            }

        } catch (Exception e) {
            log.error("[在线宝]3.发送支付请求，及获取支付请求结果出错：{}",e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[在线宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String,String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if(null!=resultListMap && !resultListMap.isEmpty()){
            if(resultListMap.size()==1){
                Map<String, String> resultMap = resultListMap.get(0);
                if(null!=resultMap && resultMap.containsKey(QRCONTEXT)){
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }
                if(null!=resultMap && resultMap.containsKey(HTMLCONTEXT)){
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayHtmlContent(resultMap.get(HTMLCONTEXT));
                }
                if(null!=resultMap && resultMap.containsKey(JUMPURL)){
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayJumpToUrl(resultMap.get(JUMPURL));
                }

            }
            if(ValidateUtil.requestesultValdata(requestPayResult)){
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            }else{
                throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        }else{
            throw  new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[在线宝]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}