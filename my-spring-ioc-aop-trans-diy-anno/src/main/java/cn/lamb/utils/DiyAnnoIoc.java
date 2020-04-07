package cn.lamb.utils;

import cn.lamb.annotation.*;
import cn.lamb.factory.BeanFactory;
import com.alibaba.druid.util.StringUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.servlet.annotation.WebServlet;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @Description 自定义注解版IoC工具类
 * @Date 2020/4/5 16:23
 * @Creator Lambert
 */
public class DiyAnnoIoc {

    private static Class[] annotations;

    static {
        List<Class> annotationList = new ArrayList<>();
        annotationList.add(MyServiceAnno.class);
        annotationList.add(MyRepositoryAnno.class);
        annotationList.add(MyComponentAnno.class);
        annotationList.add(WebServlet.class);
        annotations = new Class[annotationList.size()];
        annotationList.toArray(annotations);
    }

    /**
     * 通过注解来将bean注册到Spring
     *
     * @param singletonPool
     * @param path
     */
    public static void diyIoC(Map singletonPool, String path) {
        try {
            /*准备工作：解析xml，取component-scan节点的值为要扫描的包*/
            InputStream inputStream = BeanFactory.class.getClassLoader().getResourceAsStream(path);//以流的方式读取配置文件
            Element beansElement = new SAXReader().read(inputStream).getRootElement();
            String packagePath = beansElement.elementText("component-scan");//返回待扫描包

            /*第1步：获取所有类要注册成bean的类*/
            List<Class> classList = getAnnoClasses(packagePath, annotations);

            /*第2步：注册bean到容器*/
            for (Class<?> classClass : classList) {//遍历所有类，将其注册成组件
                addRootBean(singletonPool, classClass, annotations);
            }

            /*第3步：注入属性*/
            addAutowireBean(singletonPool);

            System.out.println();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

    }

    /****************************************第1步****************************************/
    /**
     * 获取所有类要注册成bean的类
     *
     * @param packagePath
     * @return
     */
    private static List getAnnoClasses(String packagePath, Class... annotations) {
        List<Class> classes = ClassUtils.getClasses(packagePath);
        List<Class> classList = getClassesHasAnnotation(classes, annotations);

//        String path = AnnotationUtils.class.getClassLoader().getResource(packagePath.replace(".", "/")).getPath();
//        List classPath = getClassPath(packagePath, path);
//        List<Class> classList = getClassHasAnnotation(classPath, annotations);

        return classList;
    }

    /**
     * 获取所有带有指定注解的类，只有这些类才能注册成bean
     *
     * @param classPath   所有类
     * @param annotations 指定的注解
     */
    private static List<Class> getClassesHasAnnotation(List<Class> classPath, Class... annotations) {
        List<Class> list = new ArrayList<>();
        for (Class clazz : classPath) {
            if (hasAnnotation(clazz, annotations)) {
                list.add(clazz);
            }
        }
        return list;
    }

    /**
     * 判断类是否含有指定注解
     *
     * @param clazz       待判断类
     * @param annotations 指定注解集合
     * @return
     */
    private static boolean hasAnnotation(Class<?> clazz, Class... annotations) {
        boolean result = false;
        for (Class annotation : annotations) {
            result = clazz.isAnnotationPresent(annotation) || result;
            if (result) return result;
        }
        return result;
    }


    /****************************************第2步****************************************/
    /**
     * 将最顶层的bean注册进容器，这步不管属性有无@MyAutowireAnno
     *
     * @param singletonPool
     * @param classClass
     * @param annotations
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private static void addRootBean(Map singletonPool, Class<?> classClass, Class[] annotations) {
        try {
            String beanName = getBeanName(classClass, annotations);
            Object classInstance = classClass.newInstance();
            singletonPool.put(beanName, classInstance);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取bean名称
     *
     * @param classClass  获取该类作为bean的名称
     * @param annotations 取这些注解中的代表bean名的值
     * @return
     */
    private static String getBeanName(Class<?> classClass, Class... annotations) {
        String beanName = null;
        for (Class annotation : annotations) {
            //如果是@MyServiceAnno
            if (classClass.isAnnotationPresent(annotation) && annotation == MyServiceAnno.class) {
                beanName = classClass.getAnnotation(MyServiceAnno.class).value();
            }
            //如果是@MyRepositoryAnno
            if (classClass.isAnnotationPresent(annotation) && annotation == MyRepositoryAnno.class) {
                beanName = classClass.getAnnotation(MyRepositoryAnno.class).value();
            }
            //如果没有指定别名，默认返回首字母小写的类名
            if (StringUtils.isEmpty(beanName)) {
                beanName = String.valueOf(classClass.getSimpleName().charAt(0)).toLowerCase().concat(classClass.getSimpleName().substring(1));
            }
        }
        return beanName;
    }


    /****************************************第3步****************************************/
    /**
     * 设置@MyAutowireAnno
     *
     * @param singletonPool
     */
    private static void addAutowireBean(Map singletonPool) {
        try {
            Iterator iterator = singletonPool.keySet().iterator();
            while (iterator.hasNext()) {
                Object parentBeanID = iterator.next();
                Class<?> parentClazz = singletonPool.get(parentBeanID).getClass();
                Field[] declaredFields = parentClazz.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    //如果有@MyAutowireAnno
                    if (declaredField.isAnnotationPresent(MyAutowireAnno.class)) {

                        //如果有@MyQualifierAnno指定了使用哪个具体实现类来注入
                        if (declaredField.isAnnotationPresent(MyQualifierAnno.class)) {
                            String refClassId = declaredField.getAnnotation(MyQualifierAnno.class).value();
                            Object refClassObj = singletonPool.get(refClassId);
                            injectField(singletonPool, parentBeanID, declaredField, refClassObj);
                        } else {
                            /*遍历单例池的，看看其中有没有同类型*/
                            Class<?> classType = declaredField.getType();
                            if (classType.isInterface()) {
                                //TODO 如果声明类型是接口：暂时搁置，先去实现事务再回来写
                                List<Class> allImplBaseOnInterface = ClassUtils.getAllClassByInterface(classType);
                                if (allImplBaseOnInterface.size() == 0) {
                                    throw new RuntimeException("接口".concat(classType.getName()) + "至少需要有一个实现类");
                                } else if (allImplBaseOnInterface.size() > 1) {
                                    throw new RuntimeException("接口".concat(classType.getName()) + "有多于 1 个实现类，请使用@MyQualifierAnno指定具体实现类");
                                } else {
                                    Set set = singletonPool.keySet();
                                    Iterator ir = set.iterator();
                                    while (ir.hasNext()) {
                                        Object classInstance = singletonPool.get(ir.next());
                                        Object o = allImplBaseOnInterface.get(0).newInstance();
                                        if (classInstance.getClass() == o.getClass()) {
                                            System.out.println(o);
                                            injectField(singletonPool, parentBeanID, declaredField, classInstance);
                                        }
//                                        boolean contains = values.contains();
                                    }
                                }
                            } else {
                                //如果声明类型是具体实现类
                                String fieldTypeName = classType.getSimpleName();
                                String beanID = String.valueOf(fieldTypeName.charAt(0)).toLowerCase().concat(fieldTypeName.substring(1));
                                Object refClassObj = singletonPool.get(beanID);
                                //如果之前已经有bean了，就直接设置
                                if (null != refClassObj) {
                                    injectField(singletonPool, parentBeanID, declaredField, refClassObj);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 反射注入属性
     *
     * @param singletonPool
     * @param parentBeanID
     * @param declaredField
     * @param refClassObj
     * @throws IllegalAccessException
     */
    private static void injectField(Map singletonPool, Object parentBeanID, Field declaredField, Object refClassObj) {
        try {
            declaredField.setAccessible(true);
            Object fieldRealObj = singletonPool.get(parentBeanID);
            declaredField.set(fieldRealObj, refClassObj);
            singletonPool.put(parentBeanID, fieldRealObj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
