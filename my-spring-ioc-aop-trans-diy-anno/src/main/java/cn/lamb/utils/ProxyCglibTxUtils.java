package cn.lamb.utils;

import cn.lamb.annotation.MyTransactionalAnno;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @Description 如果使用cglib动态代理，默认的MethodInterceptor不足以让我实现将注解具体到方法上，所以实现MethodInterceptor的具体类，带一个属性——原对象
 * @Date 2020/4/7 10:15
 * @Creator Lambert
 */
public class ProxyCglibTxUtils implements MethodInterceptor {

    private Object proxyObj;
    private TransactionManager transactionManager;

    public ProxyCglibTxUtils(Object proxyObj, TransactionManager transactionManager) {
        this.proxyObj = proxyObj;
        this.transactionManager = transactionManager;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        //实际开发中，可以对method做判断来决定在intercept中对功能做怎样的增强
        Method originalMethod = proxyObj.getClass().getMethod(method.getName(), method.getParameterTypes());
        //如果原方法有@MyTransactionalAnno注解，就走事务线
        if (originalMethod.isAnnotationPresent(MyTransactionalAnno.class)) {
            try {
                transactionManager.disableAutoCommit();//关闭事务自动提交
                method.invoke(proxyObj, objects);
                transactionManager.commit();//事务提交
            } catch (Exception e) {
                transactionManager.rollback();//事务回滚
                e.printStackTrace();
                throw e;//往上一层，即Servlet层抛异常，页面才不会出现“转账成功”
            }
            return null;
        }
        //没有@MyTransactionalAnno注解，就走普通反射线
        return method.invoke(proxyObj, objects);
    }
}
