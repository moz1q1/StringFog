/*
 * Copyright (C) 2017, Megatron King
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.megatronking.stringfog.plugin;

import com.github.megatronking.stringfog.IStringFog;
import com.github.megatronking.stringfog.plugin.utils.TextUtils;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

/**
 * Visit the class to execute string fog.
 *
 * @author Megatron King
 * @since 2017/3/6 20:37
 */

/* package */ class StringFogClassVisitor extends ClassVisitor {

    private static final String IGNORE_ANNOTATION = "Lcom/github/megatronking/stringfog" +
            "/annotation/StringFogIgnore;";
    private String mFogClassName;

    private boolean isClInitExists;

    private List<ClassStringField> mStaticFinalFields = new ArrayList<>();
    private List<ClassStringField> mStaticFields = new ArrayList<>();
    private List<ClassStringField> mFinalFields = new ArrayList<>();
    private List<ClassStringField> mFields = new ArrayList<>();

    private IStringFog mStringFogImpl;
    private StringFogMappingPrinter mMappingPrinter;
    private String mClassName;
    private final String mKey;

    private boolean mIgnoreClass;

    /* package */ StringFogClassVisitor(IStringFog stringFogImpl, StringFogMappingPrinter mappingPrinter,
                                        String fogClassName, String key, ClassWriter cw) {
        super(Opcodes.ASM5, cw);
        this.mStringFogImpl = stringFogImpl;
        this.mMappingPrinter = mappingPrinter;
        this.mKey = key;
        this.mFogClassName = fogClassName.replace('.', '/');
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.mClassName = name;
        //实现替换父类
        if ("android/webkit/WebView".equals(superName)) {
            System.out.println("name>>>" + name);
            System.out.println("superName>>>" + superName);
            if (!"android/webkit/FixWebView".equals(name)) {
                superName = "android/webkit/FixWebView";
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        mIgnoreClass = IGNORE_ANNOTATION.equals(desc);
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (ClassStringField.STRING_DESC.equals(desc) && name != null && !mIgnoreClass) {
            // static final, in this condition, the value is null or not null.
            if ((access & Opcodes.ACC_STATIC) != 0 && (access & Opcodes.ACC_FINAL) != 0) {
                mStaticFinalFields.add(new ClassStringField(name, (String) value));
                value = null;
            }
            // static, in this condition, the value is null.
            if ((access & Opcodes.ACC_STATIC) != 0 && (access & Opcodes.ACC_FINAL) == 0) {
                mStaticFields.add(new ClassStringField(name, (String) value));
                value = null;
            }

            // final, in this condition, the value is null or not null.
            if ((access & Opcodes.ACC_STATIC) == 0 && (access & Opcodes.ACC_FINAL) != 0) {
                mFinalFields.add(new ClassStringField(name, (String) value));
                value = null;
            }

            // normal, in this condition, the value is null.
            if ((access & Opcodes.ACC_STATIC) != 0 && (access & Opcodes.ACC_FINAL) != 0) {
                mFields.add(new ClassStringField(name, (String) value));
                value = null;
            }
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        //https://opensource.sensorsdata.cn/opensource/%e7%a5%9e%e7%ad%96-android-%e5%85%a8%e5%9f%8b%e7%82%b9%e6%8f%92%e4%bb%b6%e4%bb%8b%e7%bb%8d-%e6%95%b0%e6%8d%ae%e9%87%87%e9%9b%86/
        if (name.equals("a")
                && desc.equals("(Landroid/webkit/WebView;)V")
                && access == Opcodes.ACC_PRIVATE) {
            System.out.println("baidu lbs");
            if (mv != null) {
                //获取方法的入参
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn("searchBoxJavaBridge_");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "android/webkit/WebView", "removeJavascriptInterface", "(Ljava/lang/String;)V", false);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn("accessibility");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "android/webkit/WebView", "removeJavascriptInterface", "(Ljava/lang/String;)V", false);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn("accessibilityTraversal");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "android/webkit/WebView", "removeJavascriptInterface", "(Ljava/lang/String;)V", false);
            }
        }
//        if (name.equals("onClick") && desc.equals("(Landroid/view/View;)V")) {
//            if (mv != null) {
//                mv.visitLdcInsn("HookLog");
//                mv.visitVarInsn(Opcodes.ALOAD, 1);
//                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "android/view/View", "toString", "()Ljava/lang/String;", false);
//                mv.visitMethodInsn(INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
//            }
//        }
        if (mv != null && !mIgnoreClass) {
            //在这里插入去log的逻辑
            mv = new MethodVisitor(Opcodes.ASM5, mv) {
//                String methodName = name;
//                String methodDesc = desc;

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("i")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "i",
                                "(Ljava/lang/String;Ljava/lang/String;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("i")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "i",
                                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("v")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "v",
                                "(Ljava/lang/String;Ljava/lang/String;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("v")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "v",
                                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("d")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "d",
                                "(Ljava/lang/String;Ljava/lang/String;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("d")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "d",
                                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("w")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "w",
                                "(Ljava/lang/String;Ljava/lang/String;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("w")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/Throwable;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "w",
                                "(Ljava/lang/String;Ljava/lang/Throwable;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("w")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "w",
                                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("e")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "e",
                                "(Ljava/lang/String;Ljava/lang/String;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("e")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "e",
                                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("wtf")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "wtf",
                                "(Ljava/lang/String;Ljava/lang/String;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("wtf")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "wtf",
                                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I",
                                false);
                    } else if (owner.equalsIgnoreCase("android/util/Log")
                            && name.equalsIgnoreCase("wtf")
                            && desc.equalsIgnoreCase("(Ljava/lang/String;Ljava/lang/Throwable;)I")) {
                        //logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/github/megatronking/stringfog/log/MoL",
                                "wtf",
                                "(Ljava/lang/String;Ljava/lang/Throwable;)I",
                                false);
                    } else {
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                }
            };

            if ("<clinit>".equals(name)) {
                isClInitExists = true;
                // If clinit exists meaning the static fields (not final) would have be inited here.
                mv = new MethodVisitor(Opcodes.ASM5, mv) {

                    private String lastStashCst;

                    @Override
                    public void visitCode() {
                        super.visitCode();
                        // Here init static final fields.
                        for (ClassStringField field : mStaticFinalFields) {
                            if (!canEncrypted(field.value)) {
                                continue;
                            }
                            String originValue = field.value;
                            String encryptValue = mStringFogImpl.encrypt(originValue, mKey);
                            mMappingPrinter.output(getJavaClassName(), originValue, encryptValue);
                            super.visitLdcInsn(encryptValue);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, mFogClassName, "decrypt", "(Ljava/lang/String;)Ljava/lang/String;", false);
                            super.visitFieldInsn(Opcodes.PUTSTATIC, mClassName, field.name, ClassStringField.STRING_DESC);
                        }
                    }

                    @Override
                    public void visitLdcInsn(Object cst) {
                        // Here init static or static final fields, but we must check field name int 'visitFieldInsn'
                        if (cst != null && cst instanceof String && canEncrypted((String) cst)) {
                            lastStashCst = (String) cst;
                            String originValue = lastStashCst;
                            String encryptValue = mStringFogImpl.encrypt(originValue, mKey);
                            mMappingPrinter.output(getJavaClassName(), originValue, encryptValue);
                            super.visitLdcInsn(encryptValue);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, mFogClassName, "decrypt", "(Ljava/lang/String;)Ljava/lang/String;", false);
                        } else {
                            lastStashCst = null;
                            super.visitLdcInsn(cst);
                        }
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        if (mClassName.equals(owner) && lastStashCst != null) {
                            boolean isContain = false;
                            for (ClassStringField field : mStaticFields) {
                                if (field.name.equals(name)) {
                                    isContain = true;
                                    break;
                                }
                            }
                            if (!isContain) {
                                for (ClassStringField field : mStaticFinalFields) {
                                    if (field.name.equals(name) && field.value == null) {
                                        field.value = lastStashCst;
                                        break;
                                    }
                                }
                            }
                        }
                        lastStashCst = null;
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                };

            } else if ("<init>".equals(name)) {
                // Here init final(not static) and normal fields
                mv = new MethodVisitor(Opcodes.ASM5, mv) {
                    @Override
                    public void visitLdcInsn(Object cst) {
                        // We don't care about whether the field is final or normal
                        if (cst != null && cst instanceof String && canEncrypted((String) cst)) {
                            String originValue = (String) cst;
                            String encryptValue = mStringFogImpl.encrypt(originValue, mKey);
                            mMappingPrinter.output(getJavaClassName(), originValue, encryptValue);
                            super.visitLdcInsn(encryptValue);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, mFogClassName, "decrypt", "(Ljava/lang/String;)Ljava/lang/String;", false);
                        } else {
                            super.visitLdcInsn(cst);
                        }
                    }
                };
            } else {
                mv = new MethodVisitor(Opcodes.ASM5, mv) {

                    @Override
                    public void visitLdcInsn(Object cst) {
                        if (cst != null && cst instanceof String && canEncrypted((String) cst)) {
                            // If the value is a static final field
                            for (ClassStringField field : mStaticFinalFields) {
                                if (cst.equals(field.value)) {
                                    super.visitFieldInsn(Opcodes.GETSTATIC, mClassName, field.name, ClassStringField.STRING_DESC);
                                    return;
                                }
                            }
                            // If the value is a final field (not static)
                            for (ClassStringField field : mFinalFields) {
                                // if the value of a final field is null, we ignore it
                                if (cst.equals(field.value)) {
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitFieldInsn(Opcodes.GETFIELD, mClassName, field.name, "Ljava/lang/String;");
                                    return;
                                }
                            }
                            // local variables
                            String originValue = (String) cst;
                            String encryptValue = mStringFogImpl.encrypt(originValue, mKey);
                            mMappingPrinter.output(getJavaClassName(), originValue, encryptValue);
                            super.visitLdcInsn(encryptValue);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, mFogClassName, "decrypt", "(Ljava/lang/String;)Ljava/lang/String;", false);
                            return;
                        }
                        super.visitLdcInsn(cst);
                    }

                };
            }
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (!mIgnoreClass && !isClInitExists && !mStaticFinalFields.isEmpty()) {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            // Here init static final fields.
            for (ClassStringField field : mStaticFinalFields) {
                if (!canEncrypted(field.value)) {
                    continue;
                }
                String originValue = field.value;
                String encryptValue = mStringFogImpl.encrypt(originValue, mKey);
                mMappingPrinter.output(getJavaClassName(), originValue, encryptValue);
                mv.visitLdcInsn(encryptValue);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, mFogClassName, "decrypt", "(Ljava/lang/String;)Ljava/lang/String;", false);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, mClassName, field.name, ClassStringField.STRING_DESC);
            }
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        }
        super.visitEnd();
    }

    private boolean canEncrypted(String value) {
        // Max string length is 65535, should check the encrypted length.
        return !TextUtils.isEmptyAfterTrim(value) && !mStringFogImpl.overflow(value, mKey);
    }

    private String getJavaClassName() {
        return mClassName != null ? mClassName.replace('/', '.') : null;
    }

}
