package cn.lamb.factory;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description 解析xml文件，将所有需要用到的类实例创建并保存到容器中，供业务代码调取，对外提供获取对象实例的方法
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

            List<Element> propertyList = beansElement.selectNodes("//property");
            for (Element propertyElement : propertyList) {
                String parentId = propertyElement.getParent().attributeValue("id");
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
