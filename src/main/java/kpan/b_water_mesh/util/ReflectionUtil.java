package kpan.b_water_mesh.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings({"unchecked", "unused", "deprecation"})
public class ReflectionUtil {

    public static Class<?> getClass(String name) {
        try {
            return Class.forName(name.replace('/', '.'));
        } catch (java.lang.ClassNotFoundException e) {
            throw new ClassNotFoundException(e);
        }
    }

    public static <T> T getPublicField(Object instance, String fieldName) throws UnableToAccessFieldException {
        try {
            Field field;
            field = instance.getClass().getField(fieldName);
            return (T) field.get(instance);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new UnableToAccessFieldException(instance.getClass().getName(), e);
        }
    }

    public static <T> T getPublicStaticField(Class<?> classToAccess, String fieldName) throws UnableToAccessFieldException {
        try {
            Field field;
            field = classToAccess.getField(fieldName);
            return (T) field.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new UnableToAccessFieldException(classToAccess.getName(), e);
        }
    }

    public static <T> T getPublicStaticField(String classToAccess, String fieldName) throws UnableToAccessFieldException {
        Class<?> clazz = getClass(classToAccess);
        try {
            Field field;
            field = clazz.getField(fieldName);
            return (T) field.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new UnableToAccessFieldException(classToAccess, e);
        }
    }

    public static void setPublicField(Object instance, String fieldName, Object value) throws UnableToAccessFieldException {
        try {
            Field field;
            field = instance.getClass().getField(fieldName);
            field.set(instance, value);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new UnableToAccessFieldException(instance.getClass().getName(), e);
        }
    }

    public static <T> T invokePublicMethod(Object instance, String methodName, Object... args) {
        Class<?>[] types = fromArgs(args);
        try {
            Method method;
            method = instance.getClass().getMethod(methodName, types);
            method.setAccessible(true);
            return (T) method.invoke(instance, args);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new UnableToInvokeException(instance.getClass().getName(), e);
        }
    }

    //インライン展開されるfinal(プリミティブや文字列)は変更しても無駄

    public static <T> T getPrivateField(@Nonnull Object instance, String fieldName) throws UnableToAccessFieldException {
        return getPrivateFieldInternal(instance.getClass(), instance, fieldName);
    }

    public static <T> T getPrivateStaticField(Class<?> classToAccess, String fieldName) throws UnableToAccessFieldException {
        return getPrivateFieldInternal(classToAccess, null, fieldName);
    }

    public static <T> T getPrivateField(Class<?> classToAccess, @Nonnull Object instance, String fieldName) throws UnableToAccessFieldException {
        return getPrivateFieldInternal(classToAccess, instance, fieldName);
    }

    //"a.b.c.Class"
    public static <T> T getPrivateField(String classToAccess, @Nonnull Object instance, String fieldName) throws UnableToAccessFieldException {
        Class<?> clazz = getClass(classToAccess);
        return getPrivateField(clazz, instance, fieldName);
    }

    public static <T> T getPrivateStaticField(String classToAccess, String fieldName) throws UnableToAccessFieldException {
        Class<?> clazz = getClass(classToAccess);
        return getPrivateStaticField(clazz, fieldName);
    }

    public static void setPrivateField(@Nonnull Object instance, String fieldName, Object value) throws UnableToAccessFieldException {
        setPrivateFieldInternal(instance.getClass(), instance, fieldName, value);
    }

    public static void setPrivateStaticField(Class<?> classToAccess, String fieldName, Object value) throws UnableToAccessFieldException {
        setPrivateFieldInternal(classToAccess, null, fieldName, value);
    }

    public static void setPrivateField(Class<?> classToAccess, @Nonnull Object instance, String fieldName, Object value) throws UnableToAccessFieldException {
        setPrivateFieldInternal(classToAccess, instance, fieldName, value);
    }

    public static void setPrivateField(String classToAccess, @Nonnull Object instance, String fieldName, Object value) throws UnableToAccessFieldException {
        Class<?> clazz = getClass(classToAccess);
        setPrivateField(clazz, instance, fieldName, value);
    }

    public static void setPrivateStaticField(String classToAccess, String fieldName, Object value) throws UnableToAccessFieldException {
        Class<?> clazz = getClass(classToAccess);
        setPrivateStaticField(clazz, fieldName, value);
    }

    public static <T> T invokePrivateMethod(@Nonnull Object instance, String methodName, Class<?>[] parameterTypes, Object[] args) {
        return invokePrivateMethodInternal(instance.getClass(), instance, methodName, parameterTypes, args);
    }

    public static <T> T invokePrivateStaticMethod(Class<?> classToAccess, String methodName, Class<?>[] parameterTypes, Object[] args) {
        return invokePrivateMethodInternal(classToAccess, null, methodName, parameterTypes, args);
    }

    public static <T> T invokePrivateMethod(@Nonnull Object instance, String methodName, Object... args) {
        return invokePrivateMethodInternal(instance.getClass(), instance, methodName, args);
    }

    public static <T> T invokePrivateStaticMethod(Class<?> classToAccess, String methodName, Object... args) {
        return invokePrivateMethodInternal(classToAccess, null, methodName, args);
    }

    public static <T> T invokePrivateMethod(Class<?> classToAccess, @Nonnull Object instance, String methodName, Class<?>[] parameterTypes, Object[] args) {
        return invokePrivateMethodInternal(classToAccess, instance, methodName, parameterTypes, args);
    }

    public static <T> T invokePrivateMethod(Class<?> classToAccess, @Nonnull Object instance, String methodName, Object... args) {
        return invokePrivateMethodInternal(classToAccess, instance, methodName, args);
    }

    private static <T> T getPrivateFieldInternal(Class<?> classToAccess, @Nullable Object instance, String fieldName) throws UnableToAccessFieldException {
        try {
            Field field;
            field = classToAccess.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new UnableToAccessFieldException(classToAccess.getName(), e);
        }
    }

    private static <T> void setPrivateFieldInternal(Class<?> classToAccess, @Nullable Object instance, String fieldName, Object value) throws UnableToAccessFieldException {
        try {
            Field field;
            field = classToAccess.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new UnableToAccessFieldException(classToAccess.getName(), e);
        }
    }

    private static <T> T invokePrivateMethodInternal(Class<?> classToAccess, @Nullable Object instance, String methodName, Object... args) {
        Class<?>[] types = fromArgs(args);
        return invokePrivateMethodInternal(classToAccess, instance, methodName, types, args);
    }

    private static <T> T invokePrivateMethodInternal(Class<?> classToAccess, @Nullable Object instance, String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Method method;
            method = classToAccess.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(instance, args);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new UnableToInvokeException(classToAccess.getName(), e);
        }
    }

    //マイクラ限定Obfuscated対応のリフレクション

    public static <T> T getObfPrivateValue(Object instance, String fieldName, @Nullable String fieldSrgName) throws UnableToAccessFieldException {
        return getObfPrivateValue(instance.getClass(), instance, fieldName, fieldSrgName);
    }

    public static <T> T getObfPrivateValue(Class<?> classToAccess, @Nullable Object instance, String fieldName, @Nullable String fieldSrgName) throws UnableToAccessFieldException {
        return net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue((Class<? super Object>) classToAccess, instance, fieldName, fieldSrgName);
    }

    public static <T> void setObfPrivateValue(Class<?> classToAccess, @Nullable Object instance, @Nullable Object value, String fieldName, @Nullable String fieldSrgName) throws UnableToAccessFieldException {
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue((Class<? super Object>) classToAccess, instance, value, fieldName, fieldSrgName);
    }

    public static <T> T invokeObfPrivateMethod(Object instance, String methodName, @Nullable String methodSrgName, Object... args) throws UnableToInvokeException {
        return invokeObfPrivateMethod(instance.getClass(), instance, methodName, methodSrgName, args);
    }

    public static <T> T invokeObfPrivateMethod(Class<?> classToAccess, @Nullable Object instance, String methodName, @Nullable String methodSrgName, Object... args) throws UnableToInvokeException {
        return invokeObfPrivateMethod(classToAccess, instance, methodName, methodSrgName, fromArgs(args), args);
    }

    public static <T> T invokeObfPrivateMethod(Class<?> classToAccess, @Nullable Object instance, String methodName, @Nullable String methodSrgName, Class<?>[] parameterTypes, Object[] args) throws UnableToInvokeException {
        Method method = net.minecraftforge.fml.relauncher.ReflectionHelper.findMethod(classToAccess, methodName, methodSrgName, parameterTypes);
        try {
            return (T) method.invoke(instance, args);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new UnableToInvokeException(classToAccess.getName(), e);
        }
    }

    private static Class<?>[] fromArgs(Object... args) {
        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Class<?> type = arg.getClass();
            if (type == Boolean.class)
                type = boolean.class;
            else if (type == Integer.class)
                type = int.class;
            else if (type == Short.class)
                type = short.class;
            else if (type == Character.class)
                type = char.class;
            else if (type == Byte.class)
                type = byte.class;
            else if (type == Long.class)
                type = long.class;
            else if (type == Float.class)
                type = float.class;
            else if (type == Double.class)
                type = double.class;
            types[i] = type;
        }
        return types;
    }

    public static class ClassNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ClassNotFoundException(Exception e) { super(e); }
    }

    public static class UnableToAccessFieldException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public UnableToAccessFieldException(String className, Exception e) {
            super(e.toString() + " of class " + className, e);
        }
    }

    public static class UnableToInvokeException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public UnableToInvokeException(String className, Exception e) {
            super(e.toString() + " of class " + className, e);
        }
    }
}
