package cn.lamb.factory;

import cn.lamb.utils.ProxyCglibTxUtils;
import cn.lamb.utils.ProxyJdkTxUtils;
import cn.lamb.utils.TransactionManager;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

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
     *
     * @param obj
     * @return
     */
    public Object getJdkProxy(Object obj) {
        return Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(), new ProxyJdkTxUtils(obj, transactionManager));
    }

    /**
     * 获取CGLIB动态代理对象
     *
     * @param obj
     * @return
     */
    public Object getCglibProxy(Object obj) {
        return Enhancer.create(obj.getClass(), new ProxyCglibTxUtils(obj, transactionManager));
    }

}
