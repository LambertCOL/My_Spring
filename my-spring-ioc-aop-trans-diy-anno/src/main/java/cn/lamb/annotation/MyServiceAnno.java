package cn.lamb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description 自定义Service注解
 * @Date 2020/4/4 20:47
 * @Creator Lambert
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyServiceAnno {
    String value() default "";
}
