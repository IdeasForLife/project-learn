package com.learn.demo.java.proxy;

import java.lang.annotation.*;

/**
 * @ClassName: Intercept
 * @Description: 自定义拦截器注解
 * @Author: 尚先生
 * @CreateDate: 2019/6/17 16:28
 * @Version: 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Intercept {

    String value() default "";
}
