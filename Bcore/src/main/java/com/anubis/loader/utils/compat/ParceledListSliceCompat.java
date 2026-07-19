package com.anubis.loader.utils.compat;

import java.lang.reflect.Method;
import java.util.List;

import black.android.content.pm.BRParceledListSlice;

public class ParceledListSliceCompat {

	public static boolean isReturnParceledListSlice(Method method) {
		return method != null && method.getReturnType() == BRParceledListSlice.getRealClass();
	}

	public static boolean isParceledListSlice(Object obj) {
		return obj != null && obj.getClass() == BRParceledListSlice.getRealClass();
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> getList(Object slice) {
		if (!isParceledListSlice(slice)) {
			return null;
		}
		return (List<T>) BRParceledListSlice.get(slice).getList();
	}

	public static Object create(List<?> list) {
		Object slice = BRParceledListSlice.get()._new(list);
		if (slice != null) {
			return slice;
		} else {
			slice = BRParceledListSlice.get()._new();
		}
		for (Object item : list) {
			BRParceledListSlice.get(slice).append(item);
		}
		BRParceledListSlice.get(slice).setLastSlice(true);
		return slice;
	}
}
