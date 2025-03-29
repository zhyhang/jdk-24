package org.yanhuang.learning.jdk24;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface TestAnnotation {
    String value() default "test";
    int number() default 42;
}

@TestAnnotation(value = "classAnnotation", number = 100)
public class TestClass extends ParentClass implements TestInterface {
    // 各种访问修饰符的字段
    public static final String CONSTANT = "CONSTANT";
    private int privateField;
    protected double protectedField;
    public String publicField;
    
    // 构造方法
    public TestClass() {
        this.privateField = 0;
        this.protectedField = 0.0;
        this.publicField = "";
    }
    
    // 各种访问修饰符的方法
    @TestAnnotation(value = "methodAnnotation", number = 200)
    public void publicMethod() {
        System.out.println("Public method called");
    }
    
    private void privateMethod() {
        System.out.println("Private method called");
    }
    
    protected void protectedMethod() {
        System.out.println("Protected method called");
    }
    
    // 静态方法
    public static void staticMethod() {
        System.out.println("Static method called");
    }
    
    // 重写父类方法
    @Override
    public void interfaceMethod() {
        System.out.println("Interface method implementation");
    }
    
    // 包含复杂逻辑的方法
    public int complexMethod(int a, int b) {
        int result = a + b;
        if (result > 100) {
            result = result * 2;
        }
        return result;
    }
}

// 父类
class ParentClass {
    protected String parentField;
    
    public ParentClass() {
        this.parentField = "parent";
    }
    
    public void parentMethod() {
        System.out.println("Parent method called");
    }
}

// 接口
interface TestInterface {
    void interfaceMethod();
} 