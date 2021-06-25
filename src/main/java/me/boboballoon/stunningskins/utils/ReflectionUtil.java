package me.boboballoon.stunningskins.utils;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ReflectionUtil {

    private ReflectionUtil() {}

    /**
     * A util method used to get the current version of NMS
     *
     * @return the current version of NMS
     */
    public static String getNMSVersion() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();

        String[] elements = packageName.split("\\.");

        if (elements.length != 4) {
            return "";
        }

        return elements[3];
    }

    /**
     * Wrapper method to get a class via its name
     *
     * @param className the name of the class
     * @return the class
     */
    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className.replace("{NMS}", ReflectionUtil.getNMSVersion()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Wrapper method to get a field via its name
     *
     * @param clazz the class that contains the field
     * @param fieldName the name of the field
     * @return the field
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getField(fieldName);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Wrapper method to get a new instance of a class via the name
     *
     * @param className the name of the class
     * @param args the arguments of the constructor
     * @return a new instance of the class
     */
    public static Object newInstanceFromClass(String className, Object... args) {
        Class<?> clazz = ReflectionUtil.getClass(className);

        if (clazz == null) {
            return null;
        }

        return ReflectionUtil.newInstanceFromClass(clazz, args);
    }

    /**
     * Wrapper method to get a new instance of a class
     *
     * @param clazz he class
     * @param args the arguments of the constructor
     * @return a new instance of the class
     */
    public static Object newInstanceFromClass(Class<?> clazz, Object... args) {
        List<Class<?>> classes = new ArrayList<>();

        for (Object arg : args) {
            classes.add(arg.getClass());
        }

        Constructor<?> constructor;

        try {
            constructor = clazz.getDeclaredConstructor(classes.toArray(new Class[]{}));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }

        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Wrapper method to get a field from the given object
     *
     * @param object the given object
     * @param fieldName the name of the field
     * @return the field
     */
    public static Object getFieldFromObject(Object object, String fieldName) {
        Field field = ReflectionUtil.getField(object.getClass(), fieldName);

        if (field == null) {
            return null;
        }

        return ReflectionUtil.getValueFromField(field, object);
    }

    /**
     * Wrapper method to execute a method reflection call
     *
     * @param object the object that has the method you want to execute
     * @param methodName the name of the method
     * @param args the arguments to be passed in the method
     * @return the return value of the method
     */
    public static Object executeMethod(Object object, String methodName, Object... args) {
        List<Class<?>> classes = new ArrayList<>();

        for (Object arg : args) {
            classes.add(arg.getClass());
        }

        Method method;

        try {
            method = object.getClass().getMethod(methodName, classes.toArray(new Class[]{}));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }

        method.setAccessible(true);

        try {
            return method.invoke(object, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Wrapper method to get the value of a field from an object
     *
     * @param field the field
     * @param object the object with the field
     * @return the value of the field
     */
    public static Object getValueFromField(Field field, Object object) {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
