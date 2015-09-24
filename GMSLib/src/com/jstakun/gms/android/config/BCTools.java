package com.jstakun.gms.android.config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.engines.DESedeEngine;
import org.spongycastle.crypto.generators.PKCS12ParametersGenerator;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import com.jstakun.gms.android.utils.OsUtil;


public final class BCTools {

    private static MessageDigest md = null;
    private static CipherParameters cipherParameters = null;
    
    private static byte[] cipherData(BufferedBlockCipher cipher, byte[] data)
            throws DataLengthException, IllegalStateException, InvalidCipherTextException {
        int minSize = cipher.getOutputSize(data.length);
        byte[] outBuf = new byte[minSize];
        int length1 = cipher.processBytes(data, 0, data.length, outBuf, 0);
        int length2 = cipher.doFinal(outBuf, length1);
        int actualLength = length1 + length2;
        byte[] result = new byte[actualLength];
        System.arraycopy(outBuf, 0, result, 0, result.length);
        return result;
    }

    protected static byte[] decrypt(byte[] cipher) throws Exception {
        DESedeEngine des = new DESedeEngine();
        CBCBlockCipher des_CBC = new CBCBlockCipher(des);
        PaddedBufferedBlockCipher cipherAES = new PaddedBufferedBlockCipher(des_CBC);
        
        cipherAES.init(false, getCipherParameters());
        
        return cipherData(cipherAES, cipher);
    }

    protected static byte[] encrypt(byte[] plain) throws Exception {
        DESedeEngine des = new DESedeEngine();
        CBCBlockCipher des_CBC = new CBCBlockCipher(des);
        PaddedBufferedBlockCipher cipherAES = new PaddedBufferedBlockCipher(des_CBC);

        cipherAES.init(true, getCipherParameters());
        
        return cipherData(cipherAES, plain);
    }

    public static String getMessageDigest(String message) throws NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest hash = getMessageDigest();
        hash.update(message.getBytes());
        byte[] digest = hash.digest();
        return new String(Hex.encode(digest));
    }

    private static MessageDigest getMessageDigest() throws NoSuchAlgorithmException, NoSuchProviderException {
        if (md == null) {
            md = MessageDigest.getInstance("SHA-1");
        }
        return md;
    }
    
    private static CipherParameters getCipherParameters() {
    	if (cipherParameters == null) {
    		PKCS12ParametersGenerator pGen = new PKCS12ParametersGenerator(new SHA1Digest());
    		String salt = OsUtil.getDeviceId(ConfigurationManager.getInstance().getContext());
    		//System.out.println("Salt: " + salt + " ----------------------------");
    		char[] password = new String(Base64.decode(Commons.BC_PWD)).toCharArray();
            pGen.init(PBEParametersGenerator.PKCS12PasswordToBytes(password), Hex.decode(salt), 128);
            cipherParameters = pGen.generateDerivedParameters(192, 64);
    	}
    	return cipherParameters;
    }
}
