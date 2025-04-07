package org.yanhuang.learning.jdk24.classapi;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * 一个用于测试ClassFileToolkit功能的临时工作类
 * 尽可能包含多种类特性以验证工具类的功能
 */
@TempWorker.ClassInfo(
    author = "JDK24 Team",
    version = "1.0",
    createdDate = "2023-08-15"
)
public class TempWorker<T extends Comparable<T>> implements Runnable, Serializable, Callable<String> {
    
    // 注解定义
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ClassInfo {
        String author() default "Unknown";
        String version() default "0.1";
        String createdDate();
    }
    
    // 静态和实例字段
    private static final long serialVersionUID = 1L;
    
    @Deprecated
    private static int staticCounter = 0;
    
    private final String name;
    private int id;
    private T data;
    private List<String> messages;
    protected Map<String, Object> properties;
    public volatile boolean active;
    
    // 静态初始化块
    static {
        staticCounter = 100;
        System.out.println("静态初始化块执行");
    }
    
    // 实例初始化块
    {
        messages = new ArrayList<>();
        active = true;
        System.out.println("实例初始化块执行");
    }
    
    // 构造函数
    public TempWorker() {
        this("Default Worker");
    }
    
    public TempWorker(String name) {
        this.name = name;
        this.id = staticCounter++;
    }
    
    // 泛型构造函数
    public <V extends Number> TempWorker(String name, V value) {
        this.name = name;
        this.id = value.intValue();
    }
    
    // Getter和Setter方法
    public String getName() {
        return name;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    // 同步方法
    public synchronized void addMessage(String message) {
        messages.add(message);
    }
    
    // 静态方法
    public static int getStaticCounter() {
        return staticCounter;
    }
    
    // Runnable接口实现
    @Override
    public void run() {
        try {
            System.out.println("Worker " + name + " is running...");
            
            // 本地变量和循环
            int iterations = 5;
            for (int i = 0; i < iterations; i++) {
                // 条件判断
                if (i % 2 == 0) {
                    System.out.println("Even iteration: " + i);
                } else {
                    System.out.println("Odd iteration: " + i);
                }
                
                // 异常处理
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // switch语句
            switch (id % 3) {
                case 0:
                    addMessage("Priority: Low");
                    break;
                case 1:
                    addMessage("Priority: Medium");
                    break;
                case 2:
                    addMessage("Priority: High");
                    break;
                default:
                    addMessage("Priority: Unknown");
            }
            
            System.out.println("Worker " + name + " completed task");
        } finally {
            active = false;
        }
    }
    
    // Callable接口实现
    @Override
    public String call() throws Exception {
        run();
        return "Worker " + name + " finished with " + messages.size() + " messages";
    }
    
    // 内部类
    private class Task {
        private String description;
        
        public Task(String description) {
            this.description = description;
        }
        
        public void execute() {
            System.out.println("Executing task: " + description + " in worker: " + name);
        }
    }
    
    // 静态内部类
    public static class Factory {
        public static <E extends Comparable<E>> TempWorker<E> create(String name, E initialData) {
            TempWorker<E> worker = new TempWorker<>(name);
            worker.setData(initialData);
            return worker;
        }
    }
    
    // 泛型方法
    public <R> R processData(Function<T, R> processor) {
        if (data == null) {
            throw new IllegalStateException("Data not initialized");
        }
        return processor.apply(data);
    }
    
    // 可变参数方法
    public void processMultipleItems(String... items) {
        for (String item : items) {
            addMessage("Processing: " + item);
        }
    }
    
    // 复杂的算法方法，生成许多不同的字节码指令
    public double complexCalculation(double x, double y) {
        double result = 0;
        
        // 算术运算
        result = Math.pow(x, 2) + Math.pow(y, 2);
        
        // 条件分支
        if (x < 0 || y < 0) {
            result = Math.abs(result);
        }
        
        // 循环计算
        int steps = (int)(x + y) % 10;
        for (int i = 0; i < steps; i++) {
            result = Math.sqrt(result + i);
        }
        
        // 数组操作
        double[] values = new double[5];
        for (int i = 0; i < values.length; i++) {
            values[i] = result * (i + 1);
        }
        
        // 返回最大值
        double max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        
        return max;
    }
    
    // 使用递归的方法
    public long factorial(int n) {
        if (n <= 1) {
            return 1;
        }
        return n * factorial(n - 1);
    }
    
    // toString方法重写
    @Override
    public String toString() {
        return "TempWorker{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", active=" + active +
                ", messages=" + messages +
                ", data=" + data +
                '}';
    }
} 