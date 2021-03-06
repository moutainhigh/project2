package dc.pay.business.xianzaizhifu;

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
import sun.misc.BASE64Encoder;

import java.util.*;

@RequestPayHandler("XIANZAIZHIFU")
public final class XianZaiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XianZaiZhiFuPayRequestHandler.class);

     private static final String    seller_id = "seller_id";   //商户ID
     private static final String    order_type = "order_type"; //订单类型
     private static final String    out_trade_no = "out_trade_no"; //订单号
     private static final String    pay_body = "pay_body"; //商品描述
     private static final String    total_fee = "total_fee"; //订单金额 分
     private static final String    notify_url = "notify_url"; //回调地址
     private static final String    return_url = "return_url"; //回跳地址
     private static final String    spbill_create_ip = "spbill_create_ip"; //订单创建
     private static final String    spbill_times = "spbill_times"; //系统时间戳 格式yyyyMMddHHmmss
     private static final String    noncestr = "noncestr"; //随机字符串，不长于32位
     private static final String    remark = "remark"; //订单备注
     private static final String    sign = "sign"; //签名


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>();
        payParam.put(seller_id, channelWrapper.getAPI_MEMBERID());
        payParam.put(order_type, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(out_trade_no, channelWrapper.getAPI_ORDER_ID());
        payParam.put(pay_body, channelWrapper.getAPI_ORDER_ID());
        payParam.put(total_fee,channelWrapper.getAPI_AMOUNT());
        payParam.put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        payParam.put(return_url, channelWrapper.getAPI_WEB_URL());
        payParam.put(spbill_create_ip, channelWrapper.getAPI_Client_IP());
        payParam.put(spbill_times, System.currentTimeMillis()+"");
        payParam.put(noncestr, HandlerUtil.getRandomStr(10));
        payParam.put(remark, channelWrapper.getAPI_ORDER_ID());
        log.debug("[现在支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        ArrayList<String> list = new ArrayList<String>();
        for (Map.Entry<String , String> entry : params.entrySet()) {
            if (entry.getValue() != "" && entry.getKey() != "sign") {
                list.add(entry.getKey() + "=" + entry.getValue() + "&");
            }
        }
        int size = list.size();
        String[] arrayToSort = list.toArray(new String[size]);
        Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(arrayToSort[i]);
        }
        sb = sb.deleteCharAt(sb.length() - 1);
        try {
             pay_md5sign= RsaUtil.signByPrivateKey(sb.toString(),channelWrapper.getAPI_KEY());
        } catch (Exception e) {
             throw new PayException("[现在支付]密钥错误。");
        }
        log.debug("[现在支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==2 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper) &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), new String(new BASE64Encoder().encodeBuffer(JSON.toJSONString(payParam).getBytes())));
				if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                    result.put(HTMLCONTEXT,resultStr);
                    payResultList.add(result);
                }else{
                   JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if(null!=jsonResultStr  && jsonResultStr.containsKey("state") && "00".equalsIgnoreCase(jsonResultStr.getString("state"))
                            && jsonResultStr.containsKey("return_code") && "SUCCESS".equalsIgnoreCase(jsonResultStr.getString("return_code"))
                            && jsonResultStr.containsKey("pay_url") && StringUtils.isNotBlank(jsonResultStr.getString("pay_url"))){
                            if(HandlerUtil.isWapOrApp(channelWrapper)){
                                result.put(JUMPURL, jsonResultStr.getString("pay_url"));
                                payResultList.add(result);
                            }else{
                                result.put(QRCONTEXT, jsonResultStr.getString("pay_url"));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }
				}
                 
            }
        } catch (Exception e) { 
             log.error("[现在支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[现在支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[现在支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}