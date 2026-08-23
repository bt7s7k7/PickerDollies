package bt7s7k7.picker_dollies.support;

public final class Support {
	private Support() {}

	public static <T> Iterable<T> getIterable(Iterable<T> value) {
		return value;
	}

	public static Object getField(Object object, String fieldName, Class<?> sourceClass) {
		try {
			var field = sourceClass.getDeclaredField(fieldName);
			field.setAccessible(true);

			return field.get(object);
		} catch (NoSuchFieldException | SecurityException | IllegalAccessException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static void setField(Object object, String fieldName, Class<?> sourceClass, Object value) {
		try {
			var field = sourceClass.getDeclaredField(fieldName);
			field.setAccessible(true);

			field.set(object, value);
		} catch (NoSuchFieldException | SecurityException | IllegalAccessException exception) {
			throw new RuntimeException(exception);
		}
	}
}
