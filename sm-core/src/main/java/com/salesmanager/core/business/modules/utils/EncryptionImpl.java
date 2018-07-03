package com.salesmanager.core.business.modules.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

import com.salesmanager.core.modules.utils.Encryption;

import static javax.crypto.Cipher.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class EncryptionImpl implements Encryption {
	
	private final static String IV_P = "fedcba9876543210";
	private final static String KEY_SPEC = "AES";
	private final static String CYPHER_SPEC = "AES/CBC/PKCS5Padding";
	


    private String  secretKey;



	@Override
	public String encrypt(String value) throws Exception {
        Cipher cipher = getInstance(CYPHER_SPEC);
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), KEY_SPEC);
        IvParameterSpec ivSpec = new IvParameterSpec(IV_P
                .getBytes());
        cipher.init(ENCRYPT_MODE, keySpec, ivSpec);
        byte[] inpbytes = value.getBytes();
        byte[] encrypted = cipher.doFinal(inpbytes);
        return new String(bytesToHex(encrypted));


    }

	@Override
	public String decrypt(String value) throws Exception {


        if (isBlank(value))
            throw new Exception("Nothing to encrypt");
        Cipher cipher = getInstance(CYPHER_SPEC);
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), KEY_SPEC);
        IvParameterSpec ivSpec = new IvParameterSpec(IV_P
                .getBytes());
        cipher.init(DECRYPT_MODE, keySpec, ivSpec);
        byte[] outText;
        outText = cipher.doFinal(hexToBytes(value));
        return new String(outText);


    }
	
	
	private String bytesToHex(byte[] data) {
		if (data == null) {
			return null;
		} else {
			int len = data.length;
			String str = "";
			for (int i = 0; i < len; i++) {
				if ((data[i] & 0xFF) < 16) {
					str = str + "0"
							+ java.lang.Integer.toHexString(data[i] & 0xFF);
				} else {
					str = str + java.lang.Integer.toHexString(data[i] & 0xFF);
				}

			}
			return str;
		}
	}

	private static byte[] hexToBytes(String str) {
		if (str == null) {
			return null;
		} else if (str.length() < 2) {
			return null;
		} else {
			int len = str.length() / 2;
			byte[] buffer = new byte[len];
			for (int i = 0; i < len; i++) {
				buffer[i] = (byte) Integer.parseInt(str.substring(i * 2,
						i * 2 + 2), 16);
			}
			return buffer;
		}
	}
	
	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

}
