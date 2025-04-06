package org.yanhuang.learning.jdk24.classapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.io.Reader;
import java.lang.classfile.*;
import java.lang.constant.*;
import java.util.ArrayList;
import java.util.List;

public class ClassApiExamples {
    public static void main(String[] args) {
        try {
            // 获取当前类的Class文件路径作为测试用例
            final String currentClassPath = "src/main/java/org/yanhuang/learning/jdk24/TestClass.class";
            Path classFilePath = Paths.get(currentClassPath);
            
            System.out.println("正在分析类文件: " + classFilePath);
            System.out.println("----------------------------------------");
            
            
            // 创建ClassFileOperator实例
            ClassFileOperator operator = new ClassFileOperator();
            
            // 1. 基本分析
            System.out.println("\n1. 基本分析");
            operator.analyzeOverview(classFilePath);
            
            // 2. 分析注解
            System.out.println("\n2. 分析注解");
            operator.analyzeAnnotations(classFilePath);
            
            // 3. 分析常量池
            System.out.println("\n3. 分析常量池");
            operator.analyzeConstantPool(classFilePath);
            
            // 4. 分析方法字节码
            System.out.println("\n4. 分析方法字节码");
            operator.analyzeMethodBytecode(classFilePath);
            
            // 5. 分析继承关系
            System.out.println("\n5. 分析继承关系");
            operator.analyzeInheritance(classFilePath);
            
            // 6. 分析访问标志
            System.out.println("\n6. 分析访问标志");
            operator.analyzeAccessFlags(classFilePath);
            
            // 11. 分析方法本地变量表
            System.out.println("\n11. 分析方法本地变量表");
            operator.analyzeMethodLocalVariables(classFilePath);
            
            // 创建一个临时类文件用于修改测试
            String tempClassName = "ModifiedTestClass";
            Path tempDir = Files.createTempDirectory("jdk-24");
            Path tempClassPath = tempDir.resolve(tempClassName+".class");
            tempClassPath.toFile().deleteOnExit(); // 程序退出时删除临时文件

            // 复制原始类文件到临时目录
            Files.copy(classFilePath, tempClassPath);
            
            // 分析新创建的类文件
            System.out.println("\n新创建的类文件分析：");
            operator.analyzeOverview(tempClassPath);
            
            // 7. 添加新字段
            System.out.println("\n7. 添加新字段");
            operator.addField(tempClassPath, "newField", "Ljava/lang/String;", true, true, false);
            
            // 8. 添加新方法
            System.out.println("\n8. 添加新方法");
            operator.addMethod(tempClassPath, "newMethod", "()V", true, false);
            
            // 9. 修改方法字节码
            System.out.println("\n9. 修改方法字节码");
            operator.modifyMethodBytecode(tempClassPath, "newMethod", "()V", 
                codeBuilder -> {
                    // 添加一些简单的字节码指令
                    codeBuilder.getstatic(
                        ClassDesc.of("java/lang/System"), "out",
                        ClassDesc.of("java/io/PrintStream"));
                    codeBuilder.ldc("Hello from modified method!");
                    codeBuilder.invokevirtual(
                        ClassDesc.of("java/io/PrintStream"), "println",
                        MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V"));
                    codeBuilder.return_();
                });
            
            // 10. 修改访问标志
            System.out.println("\n10. 修改访问标志");
            operator.modifyAccessFlags(tempClassPath, ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL);
        } catch (Exception e) {
            // 处理异常
            System.err.println("分析类文件时发生错误:");
            e.printStackTrace();
        }
    }
}
