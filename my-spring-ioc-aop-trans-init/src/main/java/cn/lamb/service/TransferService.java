package cn.lamb.service;

/**
 * @author Lambert
 */
public interface TransferService {

    void transfer(String fromCardNo,String toCardNo,int money) throws Exception;
}
