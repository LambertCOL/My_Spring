package cn.lamb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description 自定义Qualifier注解
 * 仅仅用MyAutowireAnno的时候发现，当使用@MyAutowireAnno注解某个声明类型是接口的属性时，有两种情况可以向属性注入值：
 * 1.只有一个类实现了该接口
 * 2.若接口有多个实现类，必须人为指定一个实现类作为@MyAutowireAnno的默认注入类型，所以有了@MyQualifierAnno这个注解
 * @Date 2020/4/5 10:07
 * @Creator Lambert
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyQualifierAnno {
    String value() default "";
}
