package org.yanhuang.learning.jdk24;

import org.w3c.dom.html.HTMLHeadElement;

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

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface AnotherAnnotation {
    String description() default "";
}

@TestAnnotation(value = "classAnnotation", number = 100)
@AnotherAnnotation(description = "Test class for Java Class File API")
public class TestClass extends ParentClass implements TestInterface {
    // 各种访问修饰符的字段
    public static final String CONSTANT = "CONSTANT";
    private int privateField;
    protected double protectedField;
    public String publicField;
    
    // 添加更多类型的字段用于依赖分析
    private java.util.List<String> stringList;
    private java.util.Map<String, Integer> dataMap;
    protected java.time.LocalDateTime timestamp;
    
    @AnotherAnnotation(description = "Complex data structure")
    private java.util.concurrent.ConcurrentHashMap<String, java.util.List<Integer>> complexData;

    // 构造方法
    public TestClass() {
        this.privateField = 0;
        this.protectedField = 0.0;
        this.publicField = "";
        this.stringList = new java.util.ArrayList<>();
        this.dataMap = new java.util.HashMap<>();
        this.timestamp = java.time.LocalDateTime.now();
        this.complexData = new java.util.concurrent.ConcurrentHashMap<>();
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

    /**
     * 实现接口方法的方法实现，用于测试接口方法的实现情况。
     * 此方法包含多行打印语句，用于测试多行代码的分析能力。
     */
    @Override
    public void interfaceMethod() {

        System.out.println("Interface method implementation");
        System.out.println("Additional logic in interface method implementation");
        System.out.println("More logic in interface method implementation");
    }

    /**
     * 复杂方法，包含条件判断和循环，用于测试复杂逻辑的分析能力。
     * 此方法包含一个if-else-if结构，用于测试条件判断的分析能力。
     */
    public int complexMethod(int a, int b) {
        int result = a + b;
        if (result > 100) {
            result = result * 2;
        } else if (result < 0) {
            result = result - 10;
        }
        return result;
    }
    
    // 添加带有本地变量的方法用于测试本地变量表分析
    @AnotherAnnotation(description = "Method with local variables")
    public void methodWithLocalVars(String input) {
        int localInt = 42;
        double localDouble = 3.14;
        String localString = "test";
        java.util.List<String> localList = new java.util.ArrayList<>();

        for (int i = 0; i < localInt; i++) {
            localList.add(localString + i);
        }
        
        System.out.println(input + ": " + localList.size());

    }
    
    // 添加更多返回类型的方法用于依赖分析
    public java.util.Optional<String> getOptionalValue() {
        return java.util.Optional.empty();
    }
    
    public <T extends Comparable<T>> java.util.List<T> getSortedList(java.util.Collection<T> input) {
        return new java.util.ArrayList<>(input);
    }
    
    // 添加异常处理的方法用于字节码分析
    public void methodWithExceptions() throws Exception {
        try {
            throw new IllegalStateException("Test exception");
        } catch (RuntimeException e) {
            System.err.println("Caught exception: " + e.getMessage());
            throw new Exception("Wrapped exception", e);
        } finally {
            System.out.println("Finally block executed");
        }
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
