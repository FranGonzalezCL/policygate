package io.github.frangonzalezcl.policygate.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Component
public class ContextCanonicalizer {

	private final ObjectMapper canonicalMapper;

	public ContextCanonicalizer(@Qualifier("canonicalContextObjectMapper") ObjectMapper canonicalMapper) {
		this.canonicalMapper = canonicalMapper;
	}

	public String hash(Map<String, Object> context) {
		byte[] canonicalBytes = canonicalMapper.writeValueAsBytes(context);
		return HexFormat.of().formatHex(sha256(canonicalBytes));
	}

	private static byte[] sha256(byte[] input) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(input);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}

}
