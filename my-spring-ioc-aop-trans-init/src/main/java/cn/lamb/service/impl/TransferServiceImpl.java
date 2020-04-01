package cn.lamb.service.impl;

import cn.lamb.dao.AccountDao;
import cn.lamb.dao.impl.JdbcAccountDaoImpl;
import cn.lamb.pojo.Account;
import cn.lamb.service.TransferService;

/**
 * @author Lambert
 */
public class TransferServiceImpl implements TransferService {

    //1. 实例化dao层对象
    private AccountDao accountDao = new JdbcAccountDaoImpl();

    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public void transfer(String fromCardNo, String toCardNo, int money) throws Exception {

        Account from = accountDao.queryAccountByCardNo(fromCardNo);
        Account to = accountDao.queryAccountByCardNo(toCardNo);

        from.setMoney(from.getMoney() - money);
        to.setMoney(to.getMoney() + money);

        accountDao.updateAccountByCardNo(to);
        //int error = 1/0;//异常，上面执行的回滚
        accountDao.updateAccountByCardNo(from);

    }
}
