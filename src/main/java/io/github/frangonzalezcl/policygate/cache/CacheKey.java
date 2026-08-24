package io.github.frangonzalezcl.policygate.cache;

public final class CacheKey {

	private CacheKey() {
	}

	public static String of(String name, int version, String contextHash) {
		return "rule:%s:v%d:%s".formatted(name, version, contextHash);
	}

}
