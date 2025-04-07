package org.yanhuang.learning.jdk24.classapi;

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.attribute.*;
import java.lang.classfile.constantpool.*;
import java.lang.classfile.instruction.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 提供Class文件读取、写入和转换的工具类
 * 基于JDK 24的java.lang.classfile API实现
 * 
 * 注意: 这个工具类只实现了基本功能，使用时需要根据实际的JDK 24 API进行调整
 */
public class ClassFileToolkit {

    // ================= Reading classfiles =================
    
    /**
     * 读取类文件并返回ClassModel
     * 
     * @param classPath 类文件路径
     * @return 类文件模型
     * @throws IOException 如果文件读取失败
     */
    public ClassModel readClass(Path classPath) throws IOException {
        byte[] classBytes = Files.readAllBytes(classPath);
        return ClassFile.of().parse(classBytes);
    }
    
    /**
     * 读取类字节码并返回ClassModel
     * 
     * @param classBytes 类文件字节数组
     * @return 类文件模型
     */
    public ClassModel readClass(byte[] classBytes) {
        return ClassFile.of().parse(classBytes);
    }
    
    /**
     * 提取类的依赖关系
     * 提取类文件中引用的所有类
     * 
     * @param classModel 类模型
     * @return 依赖的类集合
     */
    public Set<ClassDesc> extractDependencies(ClassModel classModel) {
        Set<ClassDesc> dependencies = new HashSet<>();
        
        // 遍历常量池查找类引用
        ConstantPool cp = classModel.constantPool();
        for (int i = 1; i < cp.size(); i++) {
            try {
                PoolEntry entry = cp.entryByIndex(i);
                if (entry instanceof ClassEntry classEntry) {
                    dependencies.add(classEntry.asSymbol());
                }
            } catch (Exception e) {
                // 忽略无效的常量池项
            }
        }
            
        return dependencies;
    }
    
    /**
     * 获取类的所有方法信息
     * 
     * @param classModel 类模型
     * @return 方法信息列表
     */
    public List<Map<String, Object>> getMethodsInfo(ClassModel classModel) {
        List<Map<String, Object>> methodsInfo = new ArrayList<>();
        
        for (MethodModel methodModel : classModel.methods()) {
            Map<String, Object> methodInfo = new HashMap<>();
            methodInfo.put("name", methodModel.methodName().stringValue());
            methodInfo.put("descriptor", methodModel.methodType().stringValue());
            methodInfo.put("accessFlags", methodModel.flags().flagsMask());
            methodInfo.put("isPublic", (methodModel.flags().flagsMask() & ClassFile.ACC_PUBLIC) != 0);
            methodInfo.put("isStatic", (methodModel.flags().flagsMask() & ClassFile.ACC_STATIC) != 0);
            methodInfo.put("isNative", (methodModel.flags().flagsMask() & ClassFile.ACC_NATIVE) != 0);
            
            // 添加基本的代码信息
            for (Attribute<?> attr : methodModel.attributes()) {
                if (attr instanceof CodeAttribute) {
                    CodeAttribute codeAttr = (CodeAttribute)attr;
                    methodInfo.put("maxStack", codeAttr.maxStack());
                    methodInfo.put("maxLocals", codeAttr.maxLocals());
                    
                    // 注意: 实际使用时应根据CodeAttribute的实际API获取指令数量
                    methodInfo.put("instructionCount", 0);
                    break;
                }
            }
            
            methodsInfo.add(methodInfo);
        }
        
        return methodsInfo;
    }
    
    /**
     * 查找类中特定方法的模型
     * 
     * @param classModel 类模型
     * @param methodName 方法名
     * @param methodDescriptor 方法描述符
     * @return 方法模型的Optional
     */
    public Optional<MethodModel> findMethod(ClassModel classModel, String methodName, String methodDescriptor) {
        for (MethodModel method : classModel.methods()) {
            if (method.methodName().stringValue().equals(methodName) && 
                method.methodType().stringValue().equals(methodDescriptor)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }
    
    /**
     * 获取类的常量池信息
     * 
     * @param classModel 类模型
     * @return 常量池条目的映射列表
     */
    public List<Map<String, String>> getConstantPoolInfo(ClassModel classModel) {
        List<Map<String, String>> poolInfo = new ArrayList<>();
        ConstantPool pool = classModel.constantPool();
        
        for (int i = 1; i < pool.size(); i++) {
            try {
                PoolEntry entry = pool.entryByIndex(i);
                if (entry != null) {
                    Map<String, String> entryInfo = new HashMap<>();
                    entryInfo.put("index", String.valueOf(i));
                    entryInfo.put("type", entry.getClass().getSimpleName());
                    entryInfo.put("value", entry.toString());
                    poolInfo.add(entryInfo);
                    
                    // 处理占用两个槽位的条目
                    if (entry instanceof DoubleEntry || entry instanceof LongEntry) {
                        i++;
                    }
                }
            } catch (Exception e) {
                Map<String, String> entryInfo = new HashMap<>();
                entryInfo.put("index", String.valueOf(i));
                entryInfo.put("type", "Invalid");
                entryInfo.put("value", e.getMessage());
                poolInfo.add(entryInfo);
            }
        }
        
        return poolInfo;
    }
    
    /**
     * 获取类的本地变量表信息
     * 
     * @param methodModel 方法模型
     * @return 本地变量表信息
     */
    public List<Map<String, Object>> getLocalVariableInfo(MethodModel methodModel) {
        List<Map<String, Object>> localVars = new ArrayList<>();
        
        methodModel.findAttribute(Attributes.code())
            .ifPresent(codeAttr -> {
                codeAttr.findAttribute(Attributes.localVariableTable())
                    .ifPresent(lvtAttr -> {
                        LocalVariableTableAttribute lvt = (LocalVariableTableAttribute) lvtAttr;
                        for (LocalVariableInfo localVar : lvt.localVariables()) {
                            Map<String, Object> varInfo = new HashMap<>();
                            varInfo.put("slot", localVar.slot());
                            varInfo.put("name", localVar.name().stringValue());
                            varInfo.put("descriptor", localVar.type().stringValue());
                            varInfo.put("startPC", localVar.startPc());
                            varInfo.put("length", localVar.length());
                            localVars.add(varInfo);
                        }
                    });
            });
        
        return localVars;
    }

    // ================= Writing classfiles =================
    
    /**
     * 创建新的类文件
     * 
     * @param classPath 类文件路径
     * @param className 类名
     * @param superClass 父类（可选，默认为Object）
     * @param interfaces 接口列表（可选）
     * @param accessFlags 访问标志
     * @throws IOException 如果文件写入失败
     */
    public void createClass(Path classPath, String className, ClassDesc superClass, 
                           List<ClassDesc> interfaces, int accessFlags) throws IOException {
        ClassDesc thisClass = ClassDesc.of(className);
        ClassDesc actualSuperClass = (superClass != null) ? superClass : ClassDesc.of("java.lang.Object");
        List<ClassDesc> actualInterfaces = (interfaces != null) ? interfaces : List.of();
        
        byte[] classBytes = ClassFile.of().build(thisClass, classBuilder -> {
            // 设置访问标志
            classBuilder.withFlags(accessFlags);
            // 设置父类
            classBuilder.withSuperclass(actualSuperClass);
            // 设置接口
            classBuilder.withInterfaceSymbols(actualInterfaces);
            
            // 添加默认构造函数
            classBuilder.withMethod(
                "<init>", 
                MethodTypeDesc.ofDescriptor("()V"),
                ClassFile.ACC_PUBLIC,
                mb -> mb.withCode(cb -> {
                    cb.aload(0);
                    cb.invokespecial(
                        actualSuperClass,
                        "<init>",
                        MethodTypeDesc.ofDescriptor("()V")
                    );
                    cb.return_();
                })
            );
        });
        
        Files.write(classPath, classBytes);
    }
    
    /**
     * 向类添加字段
     * 
     * @param classModel 类模型
     * @param fieldName 字段名
     * @param fieldType 字段类型
     * @param accessFlags 访问标志
     * @return 更新后的类字节码
     */
    public byte[] addField(ClassModel classModel, String fieldName, ClassDesc fieldType, int accessFlags) {
        ClassDesc thisClass = ClassDesc.of(classModel.thisClass().asInternalName());
        
        return ClassFile.of().build(thisClass, classBuilder -> {
            // 复制原有的类信息
            classBuilder.withFlags(classModel.flags().flagsMask());
            
            // 设置父类 (简化处理)
            classBuilder.withSuperclass(ClassDesc.of("java.lang.Object"));
            
            // 复制接口 (简化处理)
            List<ClassDesc> interfaces = new ArrayList<>();
            classBuilder.withInterfaceSymbols(interfaces);
            
            // 复制原有字段
            for (FieldModel field : classModel.fields()) {
                classBuilder.withField(
                    field.fieldName().stringValue(), 
                    ClassDesc.of(field.fieldType().stringValue()),
                    field.flags().flagsMask()
                );
            }
            
            // 复制原有方法
            for (MethodModel method : classModel.methods()) {
                classBuilder.withMethod(
                    method.methodName().stringValue(),
                    MethodTypeDesc.ofDescriptor(method.methodType().stringValue()),
                    method.flags().flagsMask(),
                    mb -> {}
                );
            }
            
            // 添加新字段
            classBuilder.withField(fieldName, fieldType, accessFlags);
        });
    }
    
    /**
     * 向类添加方法
     * 
     * @param classModel 类模型
     * @param methodName 方法名
     * @param methodType 方法类型
     * @param accessFlags 访问标志
     * @param codeConsumer 代码构建器的消费者
     * @return 更新后的类字节码
     */
    public byte[] addMethod(ClassModel classModel, String methodName, MethodTypeDesc methodType, 
                           int accessFlags, Consumer<CodeBuilder> codeConsumer) {
        ClassDesc thisClass = ClassDesc.of(classModel.thisClass().asInternalName());
        
        return ClassFile.of().build(thisClass, classBuilder -> {
            // 复制原有的类信息
            classBuilder.withFlags(classModel.flags().flagsMask());
            
            // 设置父类 (简化处理)
            classBuilder.withSuperclass(ClassDesc.of("java.lang.Object"));
            
            // 复制接口 (简化处理)
            List<ClassDesc> interfaces = new ArrayList<>();
            classBuilder.withInterfaceSymbols(interfaces);
            
            // 复制原有字段
            for (FieldModel field : classModel.fields()) {
                classBuilder.withField(
                    field.fieldName().stringValue(), 
                    ClassDesc.of(field.fieldType().stringValue()),
                    field.flags().flagsMask()
                );
            }
            
            // 复制原有方法
            for (MethodModel method : classModel.methods()) {
                classBuilder.withMethod(
                    method.methodName().stringValue(),
                    MethodTypeDesc.ofDescriptor(method.methodType().stringValue()),
                    method.flags().flagsMask(),
                    mb -> {}
                );
            }
            
            // 添加新方法
            classBuilder.withMethod(
                methodName,
                methodType,
                accessFlags,
                mb -> mb.withCode(codeConsumer)
            );
        });
    }
    
    /**
     * 将类字节码写入文件
     * 
     * @param classPath 类文件路径
     * @param classBytes 类字节码
     * @throws IOException 如果文件写入失败
     */
    public void writeClass(Path classPath, byte[] classBytes) throws IOException {
        Files.write(classPath, classBytes);
    }

    // ================= Transforming classfiles =================
    
    /**
     * 修改类的访问标志
     * 
     * @param classModel 类模型
     * @param newFlags 新的访问标志
     * @return 转换后的类字节码
     */
    public byte[] modifyClassFlags(ClassModel classModel, int newFlags) {
        ClassDesc thisClass = ClassDesc.of(classModel.thisClass().asInternalName());
        
        return ClassFile.of().build(thisClass, classBuilder -> {
            // 设置新的访问标志
            classBuilder.withFlags(newFlags);
            
            // 设置父类 (简化处理)
            classBuilder.withSuperclass(ClassDesc.of("java.lang.Object"));
            
            // 复制接口 (简化处理)
            List<ClassDesc> interfaces = new ArrayList<>();
            classBuilder.withInterfaceSymbols(interfaces);
            
            // 复制原有字段
            for (FieldModel field : classModel.fields()) {
                classBuilder.withField(
                    field.fieldName().stringValue(), 
                    ClassDesc.of(field.fieldType().stringValue()),
                    field.flags().flagsMask()
                );
            }
            
            // 复制原有方法
            for (MethodModel method : classModel.methods()) {
                classBuilder.withMethod(
                    method.methodName().stringValue(),
                    MethodTypeDesc.ofDescriptor(method.methodType().stringValue()),
                    method.flags().flagsMask(),
                    mb -> {}
                );
            }
        });
    }
    
    /**
     * 修改方法的代码
     * 
     * @param classModel 类模型
     * @param methodName 要修改的方法名
     * @param methodDescriptor 方法描述符
     * @param codeBuilder 新的代码构建器
     * @return 转换后的类字节码
     */
    public byte[] modifyMethodCode(ClassModel classModel, String methodName, String methodDescriptor, 
                                 Consumer<CodeBuilder> codeBuilder) {
        ClassDesc thisClass = ClassDesc.of(classModel.thisClass().asInternalName());
        
        return ClassFile.of().build(thisClass, cb -> {
            // 复制原有的类信息
            cb.withFlags(classModel.flags().flagsMask());
            
            // 设置父类 (简化处理)
            cb.withSuperclass(ClassDesc.of("java.lang.Object"));
            
            // 复制接口 (简化处理)
            List<ClassDesc> interfaces = new ArrayList<>();
            cb.withInterfaceSymbols(interfaces);
            
            // 复制字段
            for (FieldModel field : classModel.fields()) {
                cb.withField(
                    field.fieldName().stringValue(), 
                    ClassDesc.of(field.fieldType().stringValue()),
                    field.flags().flagsMask()
                );
            }
            
            // 处理方法
            for (MethodModel method : classModel.methods()) {
                String currentMethodName = method.methodName().stringValue();
                String currentMethodDesc = method.methodType().stringValue();
                
                if (currentMethodName.equals(methodName) && currentMethodDesc.equals(methodDescriptor)) {
                    // 为目标方法创建新的代码
                    cb.withMethod(
                        currentMethodName,
                        MethodTypeDesc.ofDescriptor(currentMethodDesc),
                        method.flags().flagsMask(),
                        mb -> mb.withCode(codeBuilder)
                    );
                } else {
                    // 保留其他方法不变
                    cb.withMethod(
                        currentMethodName,
                        MethodTypeDesc.ofDescriptor(currentMethodDesc),
                        method.flags().flagsMask(),
                        mb -> {}
                    );
                }
            }
        });
    }
} 