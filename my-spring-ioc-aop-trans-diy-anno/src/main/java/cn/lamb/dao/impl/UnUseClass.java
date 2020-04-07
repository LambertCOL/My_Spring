package cn.lamb.dao.impl;

import cn.lamb.dao.AccountDao;
import cn.lamb.pojo.Account;

/**
 * @Description 这个类的唯一作用是令AccountDao有多个实现类，所以在面向接口编程时，要使用@MyAutowireAnno自动注入属性必须要@MyQualifierAnno("指定bean的id")配合
 * @Date 2020/4/7 10:53
 * @Creator Lambert
 */
public class UnUseClass implements AccountDao {
    @Override
    public Account queryAccountByCardNo(String cardNo) throws Exception {
        return null;
    }

    @Override
    public int updateAccountByCardNo(Account account) throws Exception {
        return 0;
    }
}
