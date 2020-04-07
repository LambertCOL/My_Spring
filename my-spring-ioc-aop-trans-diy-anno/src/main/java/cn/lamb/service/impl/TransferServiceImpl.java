package cn.lamb.service.impl;

import cn.lamb.annotation.MyAutowireAnno;
import cn.lamb.annotation.MyQualifierAnno;
import cn.lamb.annotation.MyServiceAnno;
import cn.lamb.annotation.MyTransactionalAnno;
import cn.lamb.dao.AccountDao;
import cn.lamb.pojo.Account;
import cn.lamb.service.TransferService;

/**
 * @author Lambert
 */
@MyServiceAnno("transferService")
@MyTransactionalAnno
public class TransferServiceImpl implements TransferService {

    @MyAutowireAnno
    @MyQualifierAnno("jdbcAccountDaoImpl")  //给AccountDao创建了两个实现类，如果使用@MyAutowireAnno是不知道将哪个实现类对象注入到accountDao，必须要@MyQualifierAnno指定
    private AccountDao accountDao;

    @Override
    @MyTransactionalAnno
    public void transfer(String fromCardNo, String toCardNo, int money) throws Exception {

        Account from = accountDao.queryAccountByCardNo(fromCardNo);
        Account to = accountDao.queryAccountByCardNo(toCardNo);

        from.setMoney(from.getMoney() - money);
        to.setMoney(to.getMoney() + money);

        accountDao.updateAccountByCardNo(to);
//        int error = 1 / 0;//异常，上面执行的回滚
        accountDao.updateAccountByCardNo(from);
    }
}
