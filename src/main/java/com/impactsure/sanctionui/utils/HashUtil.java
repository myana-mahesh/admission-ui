package com.impactsure.sanctionui.utils;


import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class HashUtil {
public static String sha256Hex(InputStream in) throws Exception {
 MessageDigest md = MessageDigest.getInstance("SHA-256");
 try (DigestInputStream dis = new DigestInputStream(in, md)) {
   byte[] buffer = new byte[8192];
   while (dis.read(buffer) != -1) {}
 }
 byte[] digest = md.digest();
 StringBuilder sb = new StringBuilder();
 for (byte b : digest) sb.append(String.format("%02x", b));
 return sb.toString();
}
}
