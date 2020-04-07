package cn.lamb.dao;

import cn.lamb.pojo.Account;

/**
 * @author Lambert
 */
public interface AccountDao {

    Account queryAccountByCardNo(String cardNo) throws Exception;

    int updateAccountByCardNo(Account account) throws Exception;
}
