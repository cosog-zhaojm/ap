package com.gao.utils;

import java.util.HashMap;
import java.util.Map;

public final class EquipmentDriveMap {
	private static Map<String, Object> map;
	static {
		map = new HashMap<String, Object>();
	}

	public static Map<String, Object> getMapObject() {
		return map;
	}
	@SuppressWarnings("unused")
	private void addMapObject(final String name, final Object o) {
		map.put(name, o);
	}

}