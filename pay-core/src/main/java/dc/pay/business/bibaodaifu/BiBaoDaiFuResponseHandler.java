package dc.pay.business.bibaodaifu;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author andrew
 * May 17, 2019
 */
@Slf4j
@ResponseDaifuHandler("BIBAODAIFU")
public final class BiBaoDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("00000");

    //2.3.1请求参数 字段名 中文名 类型 类型 说明 公共参数
    //merchantId 商户号 String(32) Y 平台分配的商户号 
//    private static final String  merchantId         = "merchantId";
    //reqNo 商户请求流水号 String(50) Y 商户请求流水，需保证在商户端 不重复。只能包含字符、数据、 下划线。 
    private static final String  reqNo         = "reqNo";
    //code 受理结果 String(5) Y 错误码，具体参见错误码对照表 此处返回码为平台受理结果，并 非最终银行处理结果。 
//    private static final String  code         = "code";
    //msg 结果描述 String(255) Y 错误信息描述 
//    private static final String  msg         = "msg";
    //cipher 业务数据密文 Text Y 业务参数加密获得 
    private static final String  cipher         = "cipher";
    //sign 签名参数 Text Y 公共参数签名获得 
    private static final String  sign         = "sign";
    
    //业务参数 (成功则返回以下数据的加密结果)
//    //reqNo 商户请求流水号 String(50) Y 商户请求流水，需保证在商户端 不重复。只能包含字符、数据、 下划线。 
//    private static final String  reqNo         = "reqNo";
    //transNo 平台交易流水号 String(30) Y 平台生成的流水号 
//    private static final String  transNo         = "transNo";
    //transAmt 交易金额 Int Y 交易金额，单位为分 
    private static final String  transAmt         = "transAmt";
    //status 代付状态 Int Y 该笔代付状态： 0-处理中 1-支付成功 2-支付失败，己退汇 该返回状态为最终银行处理交易 结果。 
    private static final String  status         = "status";
    //transTime 响应时间 String(14) Y 响应时间，年月日时分秒 固定为 yyyyMMddHmmss
//    private static final String  transTime         = "transTime";

   
    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(reqNo);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[币宝代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        Map<String, String> map = new TreeMap<>(payParam);
        map.remove(sign);
        boolean my_result = false;
        try {
            my_result = PayUtils.verify(map,payParam.get(sign),channelWrapper.getAPI_PUBLIC_KEY());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[币宝代付][代付回调]出错,错误消息：{},参数：{}",e.getMessage(),e);
            throw new PayException( String.format("[币宝代付][代付回调]出错,错误:%s",e.getMessage()));
        }
        
        log.debug("2.[币宝代付]-[代付回调]-自建签名：{}",my_result);
        return String.valueOf(my_result);
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        JSONObject jsonObj = null;
      //业务数据
        try {
            String bizContent = PayUtils.decrypt(api_response_params.get(cipher),channelWrapper.getAPI_KEY());
            jsonObj = JSON.parseObject(bizContent);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[币宝代付]-[代付回调]出错,错误消息：{},参数：{}",e.getMessage(),e);
            throw new PayException( String.format("[币宝代付]-[代付回调]出错,错误:%s",e.getMessage()));
        }
        
        boolean checkResult = false;
        String responseAmount =  jsonObj.getString(transAmt);
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[币宝代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        Boolean my_result = new Boolean(signMd5);
        
//        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[币宝代付]-[代付回调]-验证第三方签名：{}",my_result.booleanValue());
        return my_result.booleanValue();
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[币宝代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        JSONObject jsonObj = null;
      //业务数据
        try {
            String bizContent = PayUtils.decrypt(api_response_params.get(cipher),channelWrapper.getAPI_KEY());
            jsonObj = JSON.parseObject(bizContent);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[币宝代付]-[代付回调]出错,错误消息：{},参数：{}",e.getMessage(),e);
            throw new PayException( String.format("[代付回调]-[代付查询]出错,错误:%s",e.getMessage()));
        }
        
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        //status 代付状态 Int Y 该笔代付状态： 0-处理中 1-支付成功 2-支付失败，己退汇 该返回状态为最终银行处理交易 结果。
        if(jsonObj.containsKey(status)){
           if( "2".equalsIgnoreCase(jsonObj.getString(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "0".equalsIgnoreCase(jsonObj.getString(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "1".equalsIgnoreCase(jsonObj.getString(status)) ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[币宝代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}