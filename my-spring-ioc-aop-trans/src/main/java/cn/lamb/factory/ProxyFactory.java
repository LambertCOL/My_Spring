package cn.lamb.factory;

import cn.lamb.utils.TransactionManager;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Description TODO
 * @Date 2020/4/1 9:12
 * @Creator Lambert
 */
public class ProxyFactory {

    //被配置文件管理了，在这添加一个成员变量即可，不用获取TransactionManager的单例对象
    private TransactionManager transactionManager;

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * 获取JDK动态代理对象
     * @param obj
     * @return
     */
    public Object getJdkProxy(Object obj) {
        Object jdkProxyObj = Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(), new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //实际开发中，可以对method做判断来决定在invoke中对功能做怎样的增强
                try {
                    transactionManager.disableAutoCommit();//关闭事务自动提交
                    method.invoke(obj, args);
                    transactionManager.commit();//事务提交
                } catch (Exception e) {
                    transactionManager.rollback();//事务回滚
                    e.printStackTrace();
                    throw e;//往上一层，即Servlet层抛异常，页面才不会出现“转账成功”
                }
                return null;
            }
        });
        return jdkProxyObj;
    }

    /**
     * 获取CGLIB动态代理对象
     * @param obj
     * @return
     */
    public Object getCglibProxy(Object obj) {
        Object cglibProxyObj = Enhancer.create(obj.getClass(), new MethodInterceptor() {
            @Override
            //实际开发中，可以对method做判断来决定在intercept中对功能做怎样的增强
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                try {
                    transactionManager.disableAutoCommit();//关闭事务自动提交
                    method.invoke(obj, objects);
                    transactionManager.commit();//事务提交
                } catch (Exception e) {
                    transactionManager.rollback();//事务回滚
                    e.printStackTrace();
                    throw e;//往上一层，即Servlet层抛异常，页面才不会出现“转账成功”
                }
                return null;
            }
        });
        return cglibProxyObj;
    }

}
