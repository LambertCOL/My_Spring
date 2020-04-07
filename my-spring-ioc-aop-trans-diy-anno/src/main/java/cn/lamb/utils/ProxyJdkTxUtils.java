package cn.lamb.utils;

import cn.lamb.annotation.MyTransactionalAnno;
import cn.lamb.utils.TransactionManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Description 默认的InvocationHandler不足以让我实现将注解具体到方法上，所以实现InvocationHandler的具体类，带一个属性——原对象
 * @Date 2020/4/7 10:15
 * @Creator Lambert
 */
public class ProxyJdkTxUtils implements InvocationHandler {

    private Object proxyObj;
    private TransactionManager transactionManager;

    public ProxyJdkTxUtils(Object proxyObj, TransactionManager transactionManager) {
        this.proxyObj = proxyObj;
        this.transactionManager = transactionManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //实际开发中，可以对method做判断来决定在invoke中对功能做怎样的增强
        Method originalMethod = proxyObj.getClass().getMethod(method.getName(), method.getParameterTypes());
        //如果原方法有@MyTransactionalAnno注解，就走事务线
        if (originalMethod.isAnnotationPresent(MyTransactionalAnno.class)) {
            try {
                transactionManager.disableAutoCommit();//关闭事务自动提交
                method.invoke(proxyObj, args);
                transactionManager.commit();//事务提交
            } catch (Exception e) {
                transactionManager.rollback();//事务回滚
                e.printStackTrace();
                throw e;//往上一层，即Servlet层抛异常，页面才不会出现“转账成功”
            }
            return null;
        }
        //没有@MyTransactionalAnno注解，就走普通反射线
        return method.invoke(proxyObj, args);
    }
}
