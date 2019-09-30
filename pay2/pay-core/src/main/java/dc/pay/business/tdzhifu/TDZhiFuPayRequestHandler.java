package dc.pay.business.tdzhifu;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 1, 2019
 */
@RequestPayHandler("TDZHIFU")
public final class TDZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TDZhiFuPayRequestHandler.class);

    //字段名 字段说明    最大长度    是否必填    备注
    //version 版本号 3   是   固定值：1.0
    private static final String version                ="version";
    //charSet 字符集 6   是   固定：UTF8
    private static final String charSet                ="charSet";
    //remark  订单备注    32  是   订单备注说明，可与商品名称一致
    private static final String remark                ="remark";
    //returnurl   同步支付跳转  Strign(100) 是   支付成功后，通过页面跳转的方式跳转到商户指定的网站页面，例如：https://www.baidu.com/
    private static final String returnurl                ="returnurl";
    //merchno 商户号 8   是   商户注册签约后，支付平台分配的唯一标识号
    private static final String merchno                ="merchno";
    //traceno 商户订单号   32  是   由商户系统生成的唯一订单编号，最大长度为32位
    private static final String traceno                ="traceno";
    //paytype 支付方式    Strign  是   见支付方式说明
    private static final String paytype                ="paytype";
    //commodityname   商品名称    32  是   商品名称
    private static final String commodityname                ="commodityname";
    //total_fee   商户订单总金额 Number(10)  是   订单总金额以分为单位
    private static final String total_fee                ="total_fee";
    //notifyurl   通知地址    100 是   支付成功后，支付平台会主动通知商家系统，因此商家必须指定接收通知的地址，例如：    https://www.baidu.com/
    private static final String notifyurl                ="notifyurl";
    //sign    数据签名    32  是   对签名数据进行两次MD5加密后转大写
//    private static final String sign                ="sign";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version, "1.0");
                put(charSet, "UTF8");
                put(remark, "name");
                put(returnurl,channelWrapper.getAPI_WEB_URL());
                put(merchno, channelWrapper.getAPI_MEMBERID());
                put(traceno,channelWrapper.getAPI_ORDER_ID());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(commodityname, "name");
                put(total_fee,  channelWrapper.getAPI_AMOUNT());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[TD支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase());
        log.debug("[TD支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
        else{
          String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//          String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
          if (StringUtils.isBlank(resultStr)) {
              log.error("[TD支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
              //log.error("[TD支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
              //throw new PayException("返回空,参数："+JSON.toJSONString(map));
          }
          if (!resultStr.contains("{") || !resultStr.contains("}")) {
             log.error("[TD支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
             throw new PayException(resultStr);
          }
          //JSONObject jsonObject = JSONObject.parseObject(resultStr);
          JSONObject jsonObject;
          try {
              jsonObject = JSONObject.parseObject(resultStr);
          } catch (Exception e) {
              e.printStackTrace();
              log.error("[TD支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
          //只取正确的值，其他情况抛出异常
          if (null != jsonObject && jsonObject.containsKey("respCode") && "0000".equalsIgnoreCase(jsonObject.getString("respCode"))  && 
                  (jsonObject.containsKey("barUrl") && StringUtils.isNotBlank(jsonObject.getString("barUrl")) || 
                   jsonObject.containsKey("barUrl") && StringUtils.isNotBlank(jsonObject.getString("barUrl")))
                  ){
//          if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))) {
              //在扫码响应的时候 如果 jump=1 表示要跳转到指定链接。 如果 jump=0||“”，则链接可以直接拿来生成二维码
              if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                  result.put( QRCONTEXT, jsonObject.getString("barUrl"));
              }else {
                  result.put( JUMPURL, jsonObject.getString("pay_url"));
              }
          }else {
              log.error("[TD支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[TD支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[TD支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    /**
     * String(yyyy-MM-dd HH:mm:ss)转10位时间戳
     * @param time
     * @return
     */
    public static Integer StringToTimestamp(String time){

        int times = 0;
        try {  
            times = (int) ((Timestamp.valueOf(time).getTime())/1000);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        if(times==0){
            System.out.println("String转10位时间戳失败");
        }
        return times; 
        
    }
}