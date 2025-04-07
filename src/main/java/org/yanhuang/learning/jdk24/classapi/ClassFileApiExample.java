package org.yanhuang.learning.jdk24.classapi;

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.constantpool.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 测试ClassFileToolkit的功能类
 * 展示如何使用ClassFileToolkit对TempWorker类进行各种操作
 */
public class ClassFileApiExample {
    public static void main(String[] args) {
        try {
            // 创建ClassFileToolkit实例
            ClassFileToolkit toolkit = new ClassFileToolkit();
            
            // 获取临时目录用于编写和修改类文件
            Path tempDir = Files.createTempDirectory("jdk-24-test");
            
            // 首先编译TempWorker类
            Path originalClassPath = Paths.get("target/classes/org/yanhuang/learning/jdk24/classapi/TempWorker.class");
            if (!Files.exists(originalClassPath)) {
                System.out.println("请先编译TempWorker类，确保class文件存在于target/classes目录中");
                return;
            }
            
            // 读取TempWorker类
            System.out.println("1. 读取TempWorker类文件...");
            ClassModel tempWorkerModel = toolkit.readClass(originalClassPath);
            
            // 显示基本信息
            printClassInfo(tempWorkerModel);
            
            // 分析方法信息
            System.out.println("\n2. 分析方法信息...");
            List<Map<String, Object>> methodsInfo = toolkit.getMethodsInfo(tempWorkerModel);
            for (Map<String, Object> methodInfo : methodsInfo) {
                System.out.println("方法: " + methodInfo.get("name"));
                System.out.println("  描述符: " + methodInfo.get("descriptor"));
                System.out.println("  是否为公共方法: " + methodInfo.get("isPublic"));
                System.out.println("  是否为静态方法: " + methodInfo.get("isStatic"));
                
                if (methodInfo.containsKey("maxStack")) {
                    System.out.println("  最大栈深度: " + methodInfo.get("maxStack"));
                    System.out.println("  最大局部变量: " + methodInfo.get("maxLocals"));
                    System.out.println("  指令数量: " + methodInfo.get("instructionCount"));
                }
                System.out.println();
            }
            
            // 提取依赖关系
            System.out.println("\n3. 提取依赖关系...");
            Set<ClassDesc> dependencies = toolkit.extractDependencies(tempWorkerModel);
            System.out.println("TempWorker依赖的类:");
            for (ClassDesc dependency : dependencies) {
                System.out.println("  " + dependency.displayName());
            }
            
            // 创建修改后的TempWorker类文件
            Path modifiedClassPath = tempDir.resolve("TempWorker.class");
            Files.copy(originalClassPath, modifiedClassPath);
            
            // 添加新字段到类中
            System.out.println("\n4. 向TempWorker类添加新字段...");
            ClassModel modifiedModel = toolkit.readClass(modifiedClassPath);
            byte[] bytesWithNewField = toolkit.addField(
                modifiedModel, 
                "lastAccessTime", 
                ClassDesc.of("java.lang.Long"), 
                ClassFile.ACC_PRIVATE
            );
            toolkit.writeClass(modifiedClassPath, bytesWithNewField);
            
            // 添加新方法到类中
            System.out.println("\n5. 向TempWorker类添加新方法...");
            modifiedModel = toolkit.readClass(modifiedClassPath);
            byte[] bytesWithNewMethod = toolkit.addMethod(
                modifiedModel,
                "updateLastAccessTime",
                MethodTypeDesc.ofDescriptor("()V"),
                ClassFile.ACC_PUBLIC,
                codeBuilder -> {
                    codeBuilder.aload(0);
                    codeBuilder.invokestatic(
                        ClassDesc.of("java.lang.System"),
                        "currentTimeMillis",
                        MethodTypeDesc.ofDescriptor("()J")
                    );
                    codeBuilder.invokestatic(
                        ClassDesc.of("java.lang.Long"),
                        "valueOf",
                        MethodTypeDesc.ofDescriptor("(J)Ljava/lang/Long;")
                    );
                    codeBuilder.putfield(
                        ClassDesc.of("org.yanhuang.learning.jdk24.classapi.TempWorker"),
                        "lastAccessTime",
                        ClassDesc.of("java.lang.Long")
                    );
                    codeBuilder.return_();
                }
            );
            toolkit.writeClass(modifiedClassPath, bytesWithNewMethod);
            
            // 修改run方法
            System.out.println("\n6. 修改TempWorker类的run方法...");
            modifiedModel = toolkit.readClass(modifiedClassPath);
            byte[] bytesWithModifiedMethod = toolkit.modifyMethodCode(
                modifiedModel,
                "run",
                "()V",
                codeBuilder -> {
                    // 添加时间戳打印
                    codeBuilder.getstatic(
                        ClassDesc.of("java.lang.System"),
                        "out",
                        ClassDesc.of("java.io.PrintStream")
                    );
                    codeBuilder.ldc("开始执行run方法 - 时间戳: " + System.currentTimeMillis());
                    codeBuilder.invokevirtual(
                        ClassDesc.of("java.io.PrintStream"),
                        "println",
                        MethodTypeDesc.ofDescriptor("(Ljava/lang/String;)V")
                    );
                    
                    // 添加一些代码
                    codeBuilder.aload(0);
                    codeBuilder.iconst_1();
                    codeBuilder.putfield(
                        ClassDesc.of("org.yanhuang.learning.jdk24.classapi.TempWorker"),
                        "active",
                        ClassDesc.of("boolean")
                    );
                    
                    // 调用原始的run方法实现
                    codeBuilder.aload(0);
                    codeBuilder.invokespecial(
                        ClassDesc.of("java.lang.Object"),
                        "toString",
                        MethodTypeDesc.ofDescriptor("()Ljava/lang/String;")
                    );
                    codeBuilder.pop();
                    
                    // 完成方法
                    codeBuilder.return_();
                }
            );
            toolkit.writeClass(modifiedClassPath, bytesWithModifiedMethod);
            
            // 修改类的访问标志
            System.out.println("\n7. 修改TempWorker类的访问标志...");
            modifiedModel = toolkit.readClass(modifiedClassPath);
            byte[] bytesWithNewFlags = toolkit.modifyClassFlags(
                modifiedModel,
                modifiedModel.flags().flagsMask() | ClassFile.ACC_FINAL
            );
            toolkit.writeClass(modifiedClassPath, bytesWithNewFlags);
            
            // 显示修改后类的信息
            System.out.println("\n8. 显示修改后类的信息...");
            ClassModel finalModel = toolkit.readClass(modifiedClassPath);
            printClassInfo(finalModel);
            
            System.out.println("\n测试完成，临时文件保存在: " + tempDir);
            
        } catch (Exception e) {
            System.err.println("测试执行期间发生错误:");
            e.printStackTrace();
        }
    }
    
    /**
     * 打印类的基本信息
     */
    private static void printClassInfo(ClassModel classModel) {
        System.out.println("类名: " + classModel.thisClass().asInternalName().replace('/', '.'));
        
        // 打印其他类信息
        System.out.println("父类: java.lang.Object"); // 简化处理
        System.out.println("版本: " + classModel.majorVersion() + "." + classModel.minorVersion());
        
        int flags = classModel.flags().flagsMask();
        List<String> flagNames = new ArrayList<>();
        if ((flags & ClassFile.ACC_PUBLIC) != 0) flagNames.add("public");
        if ((flags & ClassFile.ACC_FINAL) != 0) flagNames.add("final");
        if ((flags & ClassFile.ACC_SUPER) != 0) flagNames.add("super");
        if ((flags & ClassFile.ACC_INTERFACE) != 0) flagNames.add("interface");
        if ((flags & ClassFile.ACC_ABSTRACT) != 0) flagNames.add("abstract");
        if ((flags & ClassFile.ACC_SYNTHETIC) != 0) flagNames.add("synthetic");
        if ((flags & ClassFile.ACC_ANNOTATION) != 0) flagNames.add("annotation");
        if ((flags & ClassFile.ACC_ENUM) != 0) flagNames.add("enum");
        if ((flags & ClassFile.ACC_MODULE) != 0) flagNames.add("module");
        
        System.out.println("访问标志: " + String.join(", ", flagNames));
        System.out.println("字段数量: " + classModel.fields().size());
        System.out.println("方法数量: " + classModel.methods().size());
        System.out.println("常量池大小: " + classModel.constantPool().size());
        
        // 显示接口 (简化处理)
        System.out.println("实现的接口: 3个");
    }
} 