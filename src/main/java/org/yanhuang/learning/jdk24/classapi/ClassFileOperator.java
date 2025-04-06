package org.yanhuang.learning.jdk24.classapi;

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.attribute.LocalVariableTableAttribute;
import java.lang.classfile.constantpool.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class ClassFileOperator {

    /**
     * 验证类文件的魔数
     * @param classBytes 类文件字节数组
     * @return 是否是有效的类文件
     */
    private boolean isValidClassFile(byte[] classBytes) {
        if (classBytes == null || classBytes.length < 4) {
            return false;
        }
        return classBytes[0] == (byte)0xCA && 
               classBytes[1] == (byte)0xFE && 
               classBytes[2] == (byte)0xBA && 
               classBytes[3] == (byte)0xBE;
    }

    /**
     * 读取并分析类文件的基本信息
     * @param classFilePath 类文件的路径
     * @throws IOException 如果文件读取失败
     */
    public void analyzeOverview(Path classFilePath) throws IOException {
        // 读取类文件内容
        byte[] classBytes = Files.readAllBytes(classFilePath);
        
        // 验证类文件
        if (!isValidClassFile(classBytes)) {
            throw new IllegalArgumentException("无效的类文件格式: " + classFilePath);
        }
        
        // 解析类文件
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        // 打印类名
        System.out.println("类名: " + classModel.thisClass().asSymbol());
        
        // 打印字段信息
        System.out.println("\n字段列表:");
        for (FieldModel field : classModel.fields()) {
            System.out.printf("- %s %s%n", 
                field.fieldType().stringValue(),
                field.fieldName().stringValue());
        }
        
        // 打印方法信息
        System.out.println("\n方法列表:");
        for (MethodModel method : classModel.methods()) {
            System.out.printf("- %s(%s) [访问标志: 0x%x]%n",
                method.methodName().stringValue(),
                method.methodType().stringValue(),
                method.flags().flagsMask());
        }
    }

    /**
     * 分析类的注解信息
     * @param classFilePath 类文件的路径
     * @throws IOException 如果文件读取失败
     */
    public void analyzeAnnotations(Path classFilePath) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        System.out.println("\n类的注解信息:");
        classModel.attributes().stream()
            .filter(attr -> attr instanceof RuntimeVisibleAnnotationsAttribute)
            .map(attr -> (RuntimeVisibleAnnotationsAttribute) attr)
            .flatMap(attr -> attr.annotations().stream())
            .forEach(annotation -> {
                System.out.printf("- @%s%n", annotation.className().stringValue());
                annotation.elements().forEach(element -> 
                    System.out.printf("  %s = %s%n", 
                        element.name().stringValue(), 
                        element.value().toString()));
            });
    }

    /**
     * 分析类的常量池信息
     * @param classFilePath 类文件的路径
     * @throws IOException 如果文件读取失败
     */
    public void analyzeConstantPool(Path classFilePath) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        System.out.println("\n常量池信息:");
        try {
            ConstantPool cp = classModel.constantPool();
            for (int i = 1; i < cp.size(); i++) {
                try {
                    var entry = cp.entryByIndex(i);
                    if (entry != null) {
                        System.out.printf("#%d = %s%n", i, entry);
                    }
                } catch (ConstantPoolException e) {
                    System.out.printf("#%d = <无法访问的常量池项: %s>%n", i, e.getMessage());
                } catch (Exception e) {
                    System.out.printf("#%d = <访问错误: %s>%n", i, e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("分析常量池时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 分析方法的字节码指令
     * @param classFilePath 类文件的路径
     * @throws IOException 如果文件读取失败
     */
    public void analyzeMethodBytecode(Path classFilePath) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        System.out.println("\n方法字节码分析:");
        for (MethodModel method : classModel.methods()) {
            System.out.printf("\n方法: %s%n", method.methodName().stringValue());
            method.attributes().stream()
                .filter(attr -> attr instanceof CodeAttribute)
                .map(attr -> (CodeAttribute) attr)
                .findFirst()
                .ifPresent(code -> {
                    System.out.println("字节码指令:");
                    code.forEach(instruction -> 
                        System.out.printf("  %s%n", instruction.toString()));
                });
        }
    }

    /**
     * 分析方法的本地变量表
     * @param classFilePath 类文件的路径
     * @throws IOException 如果文件读取失败
     */
    public void analyzeMethodLocalVariables(Path classFilePath) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);

        System.out.println("\n方法本地变量表分析:");
        for (MethodModel method : classModel.methods()) {
            System.out.printf("\n方法: %s%n", method.methodName().stringValue());

            method.attributes().stream()
                .filter(attr -> attr instanceof CodeAttribute)
                .map(attr -> (CodeAttribute) attr)
                .findFirst()
                .ifPresent(code -> {
                    System.out.println("本地变量:");
                    code.attributes().forEach(attr -> {
                        if (attr instanceof LocalVariableTableAttribute lvt) {
                            lvt.localVariables().forEach(localVar ->
                                System.out.printf("  - %s %s (范围: %d-%d, 槽位: %d)%n",
                                    localVar.type().stringValue(),
                                    localVar.name().stringValue(),
                                    localVar.startPc(),
                                    localVar.startPc() + localVar.length(),
                                    localVar.slot()));
                        }
                    });
                });
        }
    }

    /**
     * 分析类的继承关系
     * @param classFilePath 类文件的路径
     * @throws IOException 如果文件读取失败
     */
    public void analyzeInheritance(Path classFilePath) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        System.out.println("\n继承关系分析:");
        classModel.superclass().ifPresent(superClass -> 
            System.out.printf("父类: %s%n", superClass.asInternalName()));
        System.out.println("实现的接口:");
        classModel.interfaces().forEach(iface -> 
            System.out.printf("- %s%n", iface.asInternalName()));
    }

    /**
     * 分析类的访问修饰符
     * @param classFilePath 类文件的路径
     * @throws IOException 如果文件读取失败
     */
    public void analyzeAccessFlags(Path classFilePath) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        System.out.println("\n访问修饰符分析:");
        int flags = classModel.flags().flagsMask();
        System.out.printf("访问标志: 0x%x%n", flags);
        System.out.println("修饰符列表:");
        if ((flags & 0x0001) != 0) System.out.println("- public");
        if ((flags & 0x0010) != 0) System.out.println("- final");
        if ((flags & 0x0020) != 0) System.out.println("- super");
        if ((flags & 0x0200) != 0) System.out.println("- interface");
        if ((flags & 0x0400) != 0) System.out.println("- abstract");
        if ((flags & 0x1000) != 0) System.out.println("- synthetic");
        if ((flags & 0x2000) != 0) System.out.println("- annotation");
        if ((flags & 0x4000) != 0) System.out.println("- enum");
        if ((flags & 0x8000) != 0) System.out.println("- module");
    }

    /**
     * 添加新的字段到类中
     * @param classFilePath 类文件的路径
     * @param fieldName 字段名称
     * @param fieldType 字段类型
     * @param isPublic 是否为public
     * @param isStatic 是否为static
     * @param isFinal 是否为final
     * @throws IOException 如果文件读取或写入失败
     */
    public void addField(Path classFilePath, String fieldName, String fieldType, 
            boolean isPublic, boolean isStatic, boolean isFinal) throws IOException {
        if (!Files.exists(classFilePath)) {
            throw new IllegalArgumentException("类文件不存在: " + classFilePath);
        }

        // 读取类文件内容
        byte[] classBytes = Files.readAllBytes(classFilePath);
        
        // 验证类文件
        if (!isValidClassFile(classBytes)) {
            throw new IllegalArgumentException("无效的类文件格式: " + classFilePath);
        }

        try {
            ClassModel classModel = ClassFile.of().parse(classBytes);
            
            byte[] newClassBytes = ClassFile.of().build(
                ClassDesc.of(classModel.thisClass().asInternalName()),
                classBuilder -> {
                    // 复制原有的访问标志
                    classBuilder.withFlags(classModel.flags().flagsMask());
                    
                    // 复制原有的字段
                    classModel.fields().forEach(field -> 
                        classBuilder.withField(field.fieldName().stringValue(), 
                            ClassDesc.of(field.fieldType().stringValue()),
                            field.flags().flagsMask()));
                    
                    // 设置字段的访问标志
                    int accessFlags = 0;
                    if (isPublic) accessFlags |= ClassFile.ACC_PUBLIC;
                    if (isStatic) accessFlags |= ClassFile.ACC_STATIC;
                    if (isFinal) accessFlags |= ClassFile.ACC_FINAL;
                    
                    // 添加新字段
                    classBuilder.withField(fieldName, 
                        ClassDesc.of(fieldType),
                        accessFlags);
                    
                    // 复制原有的方法
                    classModel.methods().forEach(method -> 
                        classBuilder.withMethod(
                            method.methodName().stringValue(),
                            MethodTypeDesc.ofDescriptor(method.methodType().stringValue()),
                            method.flags().flagsMask(),
                            mb -> {}));
                });
            
            Files.write(classFilePath, newClassBytes);
        } catch (Exception e) {
            throw new IllegalStateException("处理类文件时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 添加新的方法到类中
     * @param classFilePath 类文件的路径
     * @param methodName 方法名称
     * @param methodDescriptor 方法描述符
     * @param isPublic 是否为public
     * @param isStatic 是否为static
     * @throws IOException 如果文件读取或写入失败
     */
    public void addMethod(Path classFilePath, String methodName, String methodDescriptor,
            boolean isPublic, boolean isStatic) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        byte[] newClassBytes = ClassFile.of().build(
            ClassDesc.of(classModel.thisClass().asInternalName()),
            classBuilder -> {
                // 复制原有的访问标志
                classBuilder.withFlags(classModel.flags().flagsMask());
                
                // 复制原有的字段和方法
                classModel.fields().forEach(field -> 
                    classBuilder.withField(field.fieldName().stringValue(), 
                        ClassDesc.of(field.fieldType().stringValue()),
                        field.flags().flagsMask()));
                
                classModel.methods().forEach(method -> 
                    classBuilder.withMethod(
                        method.methodName().stringValue(),
                        MethodTypeDesc.ofDescriptor(method.methodType().stringValue()),
                        method.flags().flagsMask(),
                        mb -> {}));
                
                // 设置方法的访问标志
                int accessFlags = 0;
                if (isPublic) accessFlags |= ClassFile.ACC_PUBLIC;
                if (isStatic) accessFlags |= ClassFile.ACC_STATIC;
                
                // 添加新方法
                classBuilder.withMethod(
                    methodName,
                    MethodTypeDesc.ofDescriptor(methodDescriptor),
                    accessFlags,
                    mb -> mb.withCode(cb -> cb.return_()));
            });
        
        Files.write(classFilePath, newClassBytes);
    }

    /**
     * 修改方法的字节码
     * @param classFilePath 类文件的路径
     * @param methodName 方法名称
     * @param methodDescriptor 方法描述符
     * @param newInstructions 新的字节码指令生成器
     * @throws IOException 如果文件读取或写入失败
     */
    public void modifyMethodBytecode(Path classFilePath, String methodName, 
            String methodDescriptor, Consumer<CodeBuilder> newInstructions) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        byte[] newClassBytes = ClassFile.of().build(
            ClassDesc.of(classModel.thisClass().asInternalName()),
            classBuilder -> {
                // 复制原有的访问标志
                classBuilder.withFlags(classModel.flags().flagsMask());
                
                // 复制字段
                classModel.fields().forEach(field -> 
                    classBuilder.withField(field.fieldName().stringValue(), 
                        ClassDesc.of(field.fieldType().stringValue()),
                        field.flags().flagsMask()));
                
                // 处理方法
                classModel.methods().forEach(method -> {
                    if (method.methodName().stringValue().equals(methodName) &&
                        method.methodType().stringValue().equals(methodDescriptor)) {
                        // 找到目标方法，创建新的方法模型
                        classBuilder.withMethod(
                            method.methodName().stringValue(),
                            MethodTypeDesc.ofDescriptor(method.methodType().stringValue()),
                            method.flags().flagsMask(),
                            mb -> mb.withCode(newInstructions));
                    } else {
                        classBuilder.withMethod(
                            method.methodName().stringValue(),
                            MethodTypeDesc.ofDescriptor(method.methodType().stringValue()),
                            method.flags().flagsMask(),
                            mb -> {});
                    }
                });
            });
        
        Files.write(classFilePath, newClassBytes);
    }

    /**
     * 添加类注解
     * @param classFilePath 类文件的路径
     * @param annotationClass 注解类名
     * @param elements 注解元素
     * @throws IOException 如果文件读取或写入失败
     */
    public void addAnnotation(Path classFilePath, String annotationClass, 
            List<DynamicConstantDesc<?>> elements) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        byte[] newClassBytes = ClassFile.of().build(
            ClassDesc.of(classModel.thisClass().asInternalName()),
            classBuilder -> {
                // 复制原有的访问标志
                classBuilder.withFlags(classModel.flags().flagsMask());
                
                // 复制字段和方法
                classModel.fields().forEach(field -> 
                    classBuilder.withField(field.fieldName().stringValue(), 
                        ClassDesc.of(field.fieldType().stringValue()),
                        field.flags().flagsMask()));
                
                classModel.methods().forEach(method -> 
                    classBuilder.withMethod(
                        method.methodName().stringValue(),
                        MethodTypeDesc.ofDescriptor(method.methodType().stringValue()),
                        method.flags().flagsMask(),
                        mb -> {}));
            });
        
        Files.write(classFilePath, newClassBytes);
    }

    /**
     * 修改类的访问标志
     * @param classFilePath 类文件的路径
     * @param newFlags 新的访问标志
     * @throws IOException 如果文件读取或写入失败
     */
    public void modifyAccessFlags(Path classFilePath, int newFlags) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        byte[] newClassBytes = ClassFile.of().build(
            ClassDesc.of(classModel.thisClass().asInternalName()),
            classBuilder -> {
                // 设置新的访问标志
                classBuilder.withFlags(newFlags);
                
                // 复制字段和方法
                classModel.fields().forEach(field -> 
                    classBuilder.withField(field.fieldName().stringValue(), 
                        ClassDesc.of(field.fieldType().stringValue()),
                        field.flags().flagsMask()));
                
                classModel.methods().forEach(method -> 
                    classBuilder.withMethod(
                        method.methodName().stringValue(),
                        MethodTypeDesc.ofDescriptor(method.methodType().stringValue()),
                        method.flags().flagsMask(),
                        mb -> {}));
            });
        
        Files.write(classFilePath, newClassBytes);
    }

    /**
     * 创建一个新的类文件
     * @param classFilePath 类文件的路径
     * @param className 类名（包含包名，例如："org.example.MyClass"）
     * @param isPublic 是否为public类
     * @param superClassName 父类名（可以为null，默认继承Object）
     * @param interfaces 要实现的接口列表（可以为null）
     * @throws IOException 如果文件写入失败
     */
    public void createNewClass(Path classFilePath, String className, boolean isPublic,
            String superClassName, List<String> interfaces) throws IOException {
        // 设置类的访问标志
        final int accessFlags = ClassFile.ACC_SUPER | (isPublic ? ClassFile.ACC_PUBLIC : 0);

        // 准备父类描述符
        final ClassDesc superClassDesc = (superClassName != null && !superClassName.isEmpty()) 
            ? ClassDesc.of(superClassName)  // 不需要手动替换点号，ClassDesc.of会处理
            : ClassDesc.of("java.lang.Object");

        // 准备接口描述符列表
        final List<ClassDesc> interfaceDescs = (interfaces != null && !interfaces.isEmpty())
            ? interfaces.stream()
                .map(ClassDesc::of)  // 直接使用ClassDesc.of，它会正确处理类名格式
                .toList()
            : List.of();

        // 创建类文件
        byte[] classBytes = ClassFile.of().build(
            ClassDesc.of(className),  // 直接使用完整类名，ClassDesc.of会处理格式转换
            classBuilder -> {
                // 设置访问标志
                classBuilder.withFlags(accessFlags);
                
                // 设置父类
                classBuilder.withSuperclass(superClassDesc);
                
                // 添加接口
                interfaceDescs.forEach(iface -> 
                    classBuilder.withInterfaceSymbols(List.of(iface)));
                
                // 添加默认构造函数
                classBuilder.withMethod("<init>", 
                    MethodTypeDesc.ofDescriptor("()V"),
                    ClassFile.ACC_PUBLIC,
                    mb -> mb.withCode(cb -> {
                        cb.aload(0);
                        cb.invokespecial(
                            superClassDesc,
                            "<init>",
                            MethodTypeDesc.ofDescriptor("()V")
                        );
                        cb.return_();
                    })
                );
            });
        
        // 写入文件
        Files.write(classFilePath, classBytes);
    }

    /**
     * 创建一个简单的Java类文件
     * @param classFilePath 类文件的路径
     * @param className 类名（包含包名）
     * @throws IOException 如果文件写入失败
     */
    public void createSimpleClass(Path classFilePath, String className) throws IOException {
        createNewClass(classFilePath, className, true, null, null);
    }

    /**
     * 分析类的依赖关系
     * @param classFilePath 类文件的路径
     * @throws IOException 如果文件读取失败
     */
    public void analyzeDependencies(Path classFilePath) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFilePath);
        ClassModel classModel = ClassFile.of().parse(classBytes);
        
        System.out.println("\n类依赖关系分析:");
        
        // 1. 父类依赖
        classModel.superclass().ifPresent(superClass -> 
            System.out.printf("父类: %s%n", superClass.asInternalName()));
        
        // 2. 接口依赖
        System.out.println("实现的接口:");
        classModel.interfaces().forEach(iface -> 
            System.out.printf("- %s%n", iface.asInternalName()));
        
        // 3. 字段类型依赖
        System.out.println("\n字段类型依赖:");
        classModel.fields().forEach(field -> 
            System.out.printf("- %s%n", field.fieldType().stringValue()));
        
        // 4. 方法签名依赖
        System.out.println("\n方法签名依赖:");
        classModel.methods().forEach(method -> {
            MethodTypeDesc methodType = MethodTypeDesc.ofDescriptor(method.methodType().stringValue());
            System.out.printf("- 方法: %s%n", method.methodName().stringValue());
            System.out.printf("  返回类型: %s%n", methodType.returnType().displayName());
            System.out.printf("  参数类型: %s%n", 
                String.join(", ", methodType.parameterList().stream()
                    .map(ClassDesc::displayName)
                    .toList()));
        });
        
        // 5. 常量池中的类引用
        System.out.println("\n常量池中的类引用:");
        try {
            ConstantPool cp = classModel.constantPool();
            for (int i = 1; i < cp.size(); i++) {
                try {
                    var entry = cp.entryByIndex(i);
                    if (entry instanceof ClassEntry classEntry) {
                        System.out.printf("#%d = %s%n", i, classEntry.asInternalName());
                    }
                } catch (Exception e) {
                    // 忽略无法访问的常量池项
                }
            }
        } catch (Exception e) {
            System.err.println("分析常量池时发生错误: " + e.getMessage());
        }
    }
}
