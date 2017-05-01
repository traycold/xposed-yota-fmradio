package com.vassoiofreddo.xposed.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


public class ReflectionUtils {
	
	public static Object get(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException, IllegalArgumentException{
		Field field;
		try {
			field = obj.getClass().getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			field = obj.getClass().getField(fieldName);
		}
		if(!field.isAccessible())
			field.setAccessible(true);
		return field.get(Modifier.isStatic(field.getModifiers())?null:obj);
	}
	
	public static Constructor get(Class cls, Class... args) throws NoSuchMethodException {
		Constructor constructor;
		try {
			constructor = cls.getDeclaredConstructor(args);
		} catch (NoSuchMethodException e) {
			constructor = cls.getConstructor(args);
		}
		if(!constructor.isAccessible())
			constructor.setAccessible(true);
		return constructor;
	}	
	
	public static MethodInvoker getMethod(Object obj, String methodName, Class... args) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method;
		try {
			method = obj.getClass().getDeclaredMethod(methodName, args);
		} catch (NoSuchMethodException e) {
			method = obj.getClass().getMethod(methodName, args);
		}
		if(!method.isAccessible())
			method.setAccessible(true);
		return new MethodInvoker(method, obj);
	}
	
	public static class MethodInvoker{
		private Object obj;
		private Method method;
		public MethodInvoker(Method method, Object obj) {
			this.obj = obj;
			this.method = method;
		}
		public Object invoke(Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
			return method.invoke(Modifier.isStatic(method.getModifiers())?null:obj, args);
		}
	}
}
