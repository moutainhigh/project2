package dc.pay.business.yongheng168;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("YONGHENGONESIXEIGHT")
public final class YongHengOneSixEightPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YongHengOneSixEightPayRequestHandler.class);

//    参数名称				参数含义				是否必填				参与签名				参数说明
//    pay_memberid			商户号				是					是					平台分配商户号
//    pay_orderid			订单号				是					是					上送订单号唯一, 字符长度20
//    pay_applydate			提交时间				是					是					时间格式：2016-12-26 18:18:18
//    pay_bankcode			银行编码				是					是					参考后续说明
//    pay_notifyurl			服务端通知			是					是					服务端返回地址.（POST返回数据）
//    pay_callbackurl		页面跳转通知			是					是					页面跳转返回地址（POST返回数据）
//    pay_amount			订单金额				是					是					商品金额
//    pay_md5sign			MD5签名				是					否					请看MD5签名字段格式
//    pay_productname		商品名称				是					否	

    private static final String pay_memberid               	="pay_memberid";
    private static final String pay_orderid           		="pay_orderid";
    private static final String pay_applydate           	="pay_applydate";
    private static final String pay_bankcode           		="pay_bankcode";
    private static final String pay_notifyurl          		="pay_notifyurl";
    private static final String pay_callbackurl             ="pay_callbackurl";
    private static final String pay_amount            		="pay_amount";
    private static final String pay_productname           	="pay_productname";
    private static final String key                 ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pay_memberid, channelWrapper.getAPI_MEMBERID());
                put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
                put(pay_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
                put(pay_productname,channelWrapper.getAPI_ORDER_ID());
                put(pay_applydate,DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
            }
        };
        log.debug("[永恆168支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        paramKeys.remove(pay_productname);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
            	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        signSrc.append(key+"="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[永恆168支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[永恆168支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[永恆168支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}