## 准备工作

初始项目：`my-spring-ioc-aop-trans-init`（供需要动手敲代码的朋友使用）

初始目录结构图

![项目目录](img\初始目录结构.png)

1. 新建名为bank的数据库，运行sql.sql建表建数据

2. 用IDEA打开本项目，在maven窗口展开my-spring-ioc-aop-trans/Plugins/tomcat7，双击tomcat7:run即可通过localhost:8080访问

3. 输入金额即可实现转账操作

   已完成项目：`my-spring-ioc-aop-trans`



## 问题1：耦合度高

![耦合度高](img\耦合1.png)
![耦合度高](img\耦合2.png)

Servlet和Service层通过`new`关键字来创建对象，使得代码高耦合，不优雅，想想有什么办法可以不用`new`关键字就可以实例化对象呢？自然可以想到反射技术。

反射技术只需要知道目标类的全限定名就可以实例化出该类的对象，而整个项目有多个需要被管理的类，那就需要将所有的类的全限定名，甚至把实例化好的对象存到一个地方，需要用时再去通过id取出一个对象，也就是说，现在我的目标从

**根据id获得类的全限定名，再去反射出一个对象 >>> 根据id直接获得类的一个实例对象**

思路的源头在“通过id获取全限定名”，所以如何做到这一步？又很自然地我可以想到用配置文件的形式。

### 1. 配置文件
我可以写个`beans.xml`来管理所有的类
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<beans>
    <bean id="transferService" class="cn.lamb.service.impl.TransferServiceImpl"></bean>
    <bean id="accountDao" class="cn.lamb.dao.impl.JdbcAccountDaoImpl"></bean>
</beans>
```
根标签为`<beans>`，在根标签下使用`<bean>`作为子标签，每个`<bean>`代表一个被管理的类。给bean标签提供两个属性：

- `id`：用来唯一标识某个类
- `class`：类的权限定名

这样一来，需要创建对象实例的地方只需要根据id就能获取到对应的class，进而反射出类创建实例。

接下来，解析xml要交给谁来做？自然要创建一个XML解析类，既然如此，不妨引入工厂模式来创建个工厂类，让工厂类来提供获取类对象方法。

### 2. BeanFactory
创建`BeanFactory`类，该类任务有二
- 解析xml文件，将所有需要用到的类实例创建并保存到容器中，供业务代码调取
- 对外提供获取对象实例的方法
```JAVA
/**
 * @Description TODO
 * @Date 2020/3/31 17:29
 * @Creator Lambert
 */
public class BeanFactory {

    public static Map map = new HashMap();


    static {
        try {
            InputStream inputStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");//以流的方式读取配置文件
            Document document = new SAXReader().read(inputStream);//将配置文件读成Document对象
            Element beansElement = document.getRootElement();//获取配置文件的beans根标签
            List<Element> beanList = beansElement.selectNodes("//bean");//获取根标签beans下的所有bean标签
            for (Element beanElement : beanList) {
                String id = beanElement.attributeValue("id");//获取bean的id属性
                String clazz = beanElement.attributeValue("class");//获取bean的class属性
                Class<?> beanClass = Class.forName(clazz);//bean代表的类
                Object beanInstance = beanClass.newInstance();//类的实例对象
                //至此获得了id和目标类的实例对象，就要将它们存起来，最理想的结构是Map。所以有了最上面定义的Map
                map.put(id, beanInstance);
            }

        } catch (DocumentException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 外部可通过getBean(提供id)方法获得对象
     *
     * @param id
     * @return
     */
    public static Object getBean(String id) {
        Object o = map.get(id);
        return o;
    }
}
```
现在，我可以改造Servlet和Service层的代码，通过`BeanFactory.getBean(id值)`来创建对象实例，而不是使用`new`关键字。

```JAVA
/*Servlet层*/
public class TransferServlet extends HttpServlet {

    //1. 实例化Service层对象
    //private TransferService transferService = new TransferServiceImpl();

    //2. 从beanFactory获取，不需要new对象
    private TransferService transferService = (TransferService) BeanFactory.getBean("transferService");

    //以下省略
}

/*Service层*/
public class TransferServiceImpl implements TransferService {

    //1. 实例化dao层对象
    //private AccountDao accountDao = new JdbcAccountDaoImpl();

    //2. 从beanFactory获取，不需要new对象
    private AccountDao accountDao = (AccountDao) BeanFactory.getBean("accountDao");

    //以下省略
}
```
测试发现，Service层报错，原因是`accountDao`为空，为什么会出现这种情况？因为我解决的问题是通过`new`创建对象，而这里的`accountDao`是Servlet层对Service的方法的调用，当中的`accountDao`作为成员变量并没有通过`new TransferServiceImpl()`而初始化，反而是在解析`beans.xml`时，`Object beanInstance = beanClass.newInstance()`为`TransferServiceImpl`类做了初始化，在那时`accountDao`还是`null`，所以既然`TransferServiceImpl`中有个`accountDao`成员，不妨我在配置文件就给它配好。

### 3. 改造配置文件

```XML
<?xml version="1.0" encoding="UTF-8" ?>
<beans>
    <bean id="transferService" class="cn.lamb.service.impl.TransferServiceImpl">
        <!--name="accountDao"指在cn.lamb.service.impl.TransferServiceImpl中的accountDao成员，ref="accountDao"指引用下面已经被管理的bean-->
        <property name="accountDao" ref="accountDao"></property>
    </bean>
    <bean id="accountDao" class="cn.lamb.dao.impl.JdbcAccountDaoImpl"></bean>
</beans>
```
在`<bean>`体编写`<property>`,`property`有两个属性：

- `name`：`<bean>`代表的类中的成员变量名
- `ref`：引用`<beans>`中已经定义过的`<bean>`

### 4. 改造BeanFactory

```JAVA
static {
        try {
            InputStream inputStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");//以流的方式读取配置文件
            Document document = new SAXReader().read(inputStream);//将配置文件读成Document对象
            Element beansElement = document.getRootElement();//获取配置文件的beans根标签
            List<Element> beanList = beansElement.selectNodes("//bean");//获取根标签beans下的所有bean标签
            for (Element beanElement : beanList) {
                String id = beanElement.attributeValue("id");//获取bean的id属性
                String clazz = beanElement.attributeValue("class");//获取bean的class属性
                Class<?> beanClass = Class.forName(clazz);//bean代表的类
                Object beanInstance = beanClass.newInstance();//类的实例对象
                //至此获得了id和目标类的实例对象，就要将它们存起来，最理想的结构是Map。所以有了最上面定义的Map
                map.put(id, beanInstance);
            }
            
            //添加如下：解析property标签
            List<Element> propertyList = beansElement.selectNodes("//property");//获取所有property标签
            for (Element propertyElement : propertyList) {
                String parentId = propertyElement.getParent().attributeValue("id");//获取property父节点的id
                Object parentInstance = map.get(parentId);//获取父节点代表的类的对象实例
                String name = propertyElement.attributeValue("name");//获取property的name属性
                String ref = propertyElement.attributeValue("ref");//获取property的ref属性
                Object refInstance = map.get(ref);//获取property引用的类的对象实例
                Method[] methods = parentInstance.getClass().getMethods();//获取父节点代表的类的所有方法，我要找到成员变量的set方法将property引用的类的对象实例反射到位
                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    if (method.getName().equalsIgnoreCase("set" + ref)) {//当遍历到对应的set方法
                        method.invoke(parentInstance, refInstance);//由父节点代表的类对象实例执行method，也就是set方法，带的参数是refInstance
                    }
                }
                map.put(parentId, parentInstance);//最后将带有成员变量且初始化好的父节点对象重新放回ioc容器
            }

        } catch (DocumentException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
```

### 5. 改造Service层
```JAVA
public class TransferServiceImpl implements TransferService {

    //1. 实例化dao层对象
    //private AccountDao accountDao = new JdbcAccountDaoImpl();

    //2. 从beanFactory获取，不需要new对象
    private AccountDao accountDao;

    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    //以下省略
}
```
为成员变量`accountDao`添加set方法，好让`BeanFactory`能通过反射执行set方法去给`accountDao`赋值。

**至此IOC实践完毕**



## 问题2：无事务处理
这里的示例是转账，说到转账就要提到事务——我给你转账，我账户内被扣除的数额必须等于你账户内新加的数额，要么大家都变，要么大家都不变，不允许存在其中一方改变另一方不变的情况。

于是我来人为地使坏，在Service层的转账方法transfer中的两个更新账户方法之间添加一个异常：
```JAVA
public void transfer(String fromCardNo, String toCardNo, int money) throws Exception {

    Account from = accountDao.queryAccountByCardNo(fromCardNo);
    Account to = accountDao.queryAccountByCardNo(toCardNo);

    from.setMoney(from.getMoney() - money);
    to.setMoney(to.getMoney() + money);

    accountDao.updateAccountByCardNo(to);
    int error = 1/0;//1除0必须报错
    accountDao.updateAccountByCardNo(from);

}
```
再去测试一下，浏览器会告诉我转账失败，但我查数据库数据的时候可以发现，收款方账户的数额改变而付款方的没变，这也正是因为在代码中异常之前的代码执行完毕并提交到数据库，异常抛出后其后的代码不再执行。

系统功能会出现这种bug可不行，转入转出操作必须同时成功或失败，那为什么会成功一半呢？从代码角度来看，是因为和数据库做交互那层，就是Dao层创建数据库连接的时候，连接默认是自动提交，也就是每执行一条SQL，就马上更新到数据库（若想求证也可以打印`con.getAutoCommit()`，会返回true）：
```JAVA
public int updateAccountByCardNo(Account account) throws Exception {
    // 从连接池获取连接
    Connection con = DruidUtils.getInstance().getConnection();
    String sql = "update account set money=? where cardNo=?";
    PreparedStatement preparedStatement = con.prepareStatement(sql);
    preparedStatement.setInt(1, account.getMoney());
    preparedStatement.setString(2, account.getCardNo());
    int i = preparedStatement.executeUpdate();
    preparedStatement.close();
    con.close();
    return i;
}
```
那我关掉`con`的自动提交就好了吗？也没那么简单，如果关闭了自动提交，那什么时候调用`con.commit()`进行手动提交呢？我需要的是两次update都没问题后再提交，但若如此，两次update都各自创建了连接，应该用哪个进行提交操作呢？所以思路应该走向**用同一个连接做两次update操作最后再提交**。那么如何保证两次操作用的是同一个连接？这里要提出个新工具：`ThreadLocal`类。

`ThreadLocal`官方解释：

![官方解释](img\ThreadLocal官方解释.png)

挑重点简单翻译：`ThreadLocal`类提供线程局部变量，说大白话就是让变量和线程挂钩，可以通过`ThreadLocal`获取同个线程下的变量。

### 1. ConnectionUtils

新建一个工具类`ConnectionUtils`来处理线程和数据库连接的事：

```JAVA
public class ConnectionUtils {

    //写成单例类，连接工具只要一个就够了
    private ConnectionUtils(){}

    private static ConnectionUtils connectionUtils = new ConnectionUtils();

    public static ConnectionUtils getInstance() {
        return  connectionUtils;
    }

    ThreadLocal<Connection> threadLocal = new ThreadLocal<>();//将当前连接和线程挂钩

    /**
     * 对外提供获取与当前线程挂钩的数据库连接
     */
    public Connection getCurrentThreadConn() throws SQLException {
        Connection connection = threadLocal.get();//通过调用ThreadLocal实例的get方法可以获取与其挂钩的Connection
        if (connection == null) {//如果当前的Connection为空，说明是当前线程第一次来获取连接
            connection = DruidUtils.getInstance().getConnection();//第一次可以从线程池拿
        }
        return connection;//否则就是第一次以后来获取连接，那么直接返回
    }

}

```
这样一来，Dao层中获取连接的方式也要发生改变：**从连接池获取连接 >>> 从当前线程当中获取绑定的connection连接**：
```JAVA
public int updateAccountByCardNo(Account account) throws Exception {

    // 从连接池获取连接
    // Connection con = DruidUtils.getInstance().getConnection();
    // 改造为：从当前线程当中获取绑定的connection连接
    Connection con = ConnectionUtils.getInstance().getCurrentThreadConn();
    con.setAutoCommit(false);//关闭自动提交
    String sql = "update account set money=? where cardNo=?";
    PreparedStatement preparedStatement = con.prepareStatement(sql);
    preparedStatement.setInt(1, account.getMoney());
    preparedStatement.setString(2, account.getCardNo());
    int i = preparedStatement.executeUpdate();

    preparedStatement.close();
    //con.close();//数据库连接不能关闭，若关闭，同事务的操作得到的就不是同一个连接
    return i;
}
```
至此可以保证数据库连接的一致了，有关事务的业务代码逻辑应该是这样：
```JAVA
try {
    关闭事务的自动提交
    做业务处理
    业务处理没问题，手动提交
} catch (Exception e) {
    异常报错，try块的业务不作数，事务回滚
}
```
### 2. TransactionManager

基于面向对象思想，我再创建一个事务管理类`TransactionManager`来做事务管理，将关闭自动提交、手动提交、事务回滚写成方法以供调用：

```JAVA
public class TransactionManager {
    
    //写成单例类，事务管理器只要一个就够了
    private TransactionManager(){}

    private static TransactionManager transactionManager = new TransactionManager();

    public static TransactionManager getInstance() {
        return  transactionManager;
    }

    /**
     * 关闭自动提交
     * @throws SQLException
     */
    public void disableAutoCommit() throws SQLException {
        ConnectionUtils.getInstance().getCurrentThreadConn().setAutoCommit(false);
    }

    /**
     * 提交
     * @throws SQLException
     */
    public void commit() throws SQLException {
        ConnectionUtils.getInstance().getCurrentThreadConn().commit();
    }

    /**
     * 回滚
     * @throws SQLException
     */
    public void rollback() throws SQLException {
        ConnectionUtils.getInstance().getCurrentThreadConn().rollback();
    }

}
```
修改Service层代码：
```JAVA
/**
 * 转账操作
 */
public void transfer(String fromCardNo, String toCardNo, int money) throws Exception {
    try {
        TransactionManager.getInstance().disableAutoCommit();//关闭事务自动提交
        Account from = accountDao.queryAccountByCardNo(fromCardNo);
        Account to = accountDao.queryAccountByCardNo(toCardNo);

        from.setMoney(from.getMoney() - money);
        to.setMoney(to.getMoney() + money);

        accountDao.updateAccountByCardNo(to);
        //int error = 1/0;//异常，上面执行的回滚
        accountDao.updateAccountByCardNo(from);
        TransactionManager.getInstance().commit();//事务提交
    } catch (Exception e) {
        e.printStackTrace();
        TransactionManager.getInstance().rollback();//事务回滚
        throw e;//往上一层，即Servlet层抛异常，页面才不会出现“转账成功”
    }
}
```
分别测试注释和不注释`int error = 1/0;`可以发现转账的输入输出操作结果保持一致。

**至此事务实践完毕**



## 问题3：方法违背单一原则

改造原有代码至此，功能已经完善，那还有什么可以改进的地方吗？答案是肯定的。看看Service层代码：

![原有逻辑](img/真正的业务.png)

转账方法`transfer`的真正业务逻辑只有方法体中红框部分，其余都是与事务相关的代码，而与事务相关的代码是不能放在Service层但又是必需的，所以我的目标是将代码优化成**Service层只写业务逻辑，但是业务逻辑中要包含事务代码，同时事务代码不能出现在Service层**，这个要求看起来很迷，却是合理且科学的。

分析一下现在Service层的代码，抽象来看，问题实质上是**在做正事（业务逻辑）前先做另一件事（此处是关闭事务自动提交），在做完正事后又要做第三件事（手动提交或回滚）**，要实现这种模型我一下子就想到了拦截器，拦截器的实现是基于代理模式思想，所以上述要求可以通过代理来做。

代理模式是将原本要Service层做的事交给它做，但是代码还是可以写在Service层的，我要的就是Service层一点事务代码都不留，所以要**抽取出事务代码放至别处**，这么做的原因有二：1.净化Service层，2.假设有多个方法需要做业务处理，在每个Service层方法中都写上代理对象的生成代码是不现实的。因此，我专门创建一个代理工厂类ProxyFactory来负责提供代理对象。这里的抽取就是一个横向的抽取，体现的就是面向切向思想，即AOP。

### 1. ProxyFactory

代理模式不是这里的重点所以不展开描述，简单来说代理模式分为静态代理和动态代理，静态代理需要专门为每一个需代理的类定制一个类来实现功能增强，也就是说，要为每个要实现功能增强的类多创建一个代理类，当下我并不需要用这种方式（用在这里不科学），所以我选用动态代理，而动态动力又分为jdk动态代理和cglib动态代理，它们二者最大的区别是被代理类是否实现接口，用哪个呢？小孩子才做选择——

![我全都要](img/我全都要.gif)

创建一个代理工厂，这里提供两种获取代理对象方式：

```JAVA
public class ProxyFactory {

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
                    TransactionManager.getInstance().disableAutoCommit();//关闭事务自动提交
                    method.invoke(obj, args);
                    TransactionManager.getInstance().commit();//事务提交
                } catch (Exception e) {
                    TransactionManager.getInstance().rollback();//事务回滚
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
                    TransactionManager.getInstance().disableAutoCommit();//关闭事务自动提交
                    method.invoke(obj, objects);
                    TransactionManager.getInstance().commit();//事务提交
                } catch (Exception e) {
                    TransactionManager.getInstance().rollback();//事务回滚
                    e.printStackTrace();
                    throw e;//往上一层，即Servlet层抛异常，页面才不会出现“转账成功”
                }
                return null;
            }
        });
        return cglibProxyObj;
    }

}
```

这样一来，就可以让代理对象在执行原方法的业务代码之前先关闭事务自动提交，之后做事务提交，有异常则事务回滚且将异常往上层抛出。

### 2. 改造Servlet和Service

既然可以获取动态代理对象，那Servlet层就不需要直接获得一个Service层对象了，而是通过代理工厂获取代理对象，让代理对象干活。

```JAVA
public class TransferServlet extends HttpServlet {

    //1. 实例化Service层对象
    //private TransferService transferService = new TransferServiceImpl();

    //2. 从beanFactory获取，不需要new对象
    //private TransferService transferService = (TransferService) BeanFactory.getBean("transferService");

    private ProxyFactory proxyFactory = new ProxyFactory();
    
    //3. 获取动态代理对象，而不使用原对象
    private TransferService transferService = (TransferService) proxyFactory.getJdkProxy(BeanFactory.getBean("transferService")) ;
    
    //以下省略
}
```

Service层也可以专心写业务代码，不需要管什么事务了。

```JAVA
public void transfer(String fromCardNo, String toCardNo, int money) throws Exception 
    Account from = accountDao.queryAccountByCardNo(fromCardNo);
    Account to = accountDao.queryAccountByCardNo(toCardNo);

    from.setMoney(from.getMoney() - money);
    to.setMoney(to.getMoney() + money);

    accountDao.updateAccountByCardNo(to);
    //int error = 1/0;//异常，上面执行的回滚
    accountDao.updateAccountByCardNo(from);
}
```

测试验证，效果不变，但是代码更优雅了，这就是。

**至此AOP实践完毕**

## 完善工作

回顾我的代码，还有什么地方需要改进的吗？有的，要将IOC贯彻到底。在处理事务和横向抽取代码的时候，我又多加了三个类：`ConnectionUtils`（负责提供与线程相关的数据库连接）、`TransactionManager`（负责事务处理）、`ProxyFactory`（负责提供代理对象）。在使用时又出现了相互依赖的情况，我最先做的就是实现IOC，不能在最后打自己脸吧，所以把它们统统赶到`beans.xml`管理起来，各处引用都完善一下才是最优状态。

### 1. 改造配置文件

```XML
<?xml version="1.0" encoding="UTF-8" ?>
<beans>
    <!--Service层-->
    <bean id="transferService" class="cn.lamb.service.impl.TransferServiceImpl">
        <!--name="accountDao"指在cn.lamb.service.impl.TransferServiceImpl中的accountDao成员，ref="accountDao"指引用下面已经被管理的bean-->
        <property name="accountDao" ref="accountDao"></property>
    </bean>

    <!--Dao层-->
    <bean id="accountDao" class="cn.lamb.dao.impl.JdbcAccountDaoImpl">
        <property name="connectionUtils" ref="connectionUtils"></property>
    </bean>

    <!--代理工厂-->
    <bean id="proxyFactory" class="cn.lamb.factory.ProxyFactory">
        <property name="transactionManager" ref="transactionManager"></property>
    </bean>

    <!--数据库连接工具类-->
    <bean id="connectionUtils" class="cn.lamb.utils.ConnectionUtils"></bean>

    <!--事务管理器-->
    <bean id="transactionManager" class="cn.lamb.utils.TransactionManager">
        <property name="connectionUtils" ref="connectionUtils"></property>
    </bean>
</beans>
```

### 2. 改造Servlet

```JAVA
public class TransferServlet extends HttpServlet {

    //1. 实例化Service层对象
    //private TransferService transferService = new TransferServiceImpl();

    //2. 从beanFactory获取，不需要new对象
    //private TransferService transferService = (TransferService) BeanFactory.getBean("transferService");

    //3. 从beanFactory获取proxyBean
    private ProxyFactory proxyFactory = (ProxyFactory) BeanFactory.getBean("proxyFactory");

    //4. 再由proxyBean构建一个TransferService类型的代理对象来调用Service层方法，在Service层原方法之上增强一些功能（这里指事务）
    private TransferService transferService = (TransferService) proxyFactory.getJdkProxy(BeanFactory.getBean("transferService")) ;
    
    //以下省略
}
```

### 3.  去掉单例

```JAVA
public class ProxyFactory {
    
    //被配置文件管理了，在这添加一个成员变量即可，不用获取TransactionManager的单例对象
    private TransactionManager transactionManager;

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
    //以下省略
}

/**去掉单例写法以便管理**/
public class TransactionManager {

    //被配置文件管理了，在这添加一个成员变量即可，不用获取ConnectionUtils的单例对象
    private ConnectionUtils connectionUtils;

    public void setConnectionUtils(ConnectionUtils connectionUtils) {
        this.connectionUtils = connectionUtils;
    }
    //以下省略
}

/**ConnectionUtils也去掉单例写法以便管理**/
```

欢迎读者来捶

![微信二维码](img/微信二维码.jpg)