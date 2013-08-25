/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.config;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.util.encoders.Hex;


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
            pGen.init(PBEParametersGenerator.PKCS12PasswordToBytes(Commons.BC_PASSWORD.toCharArray()), Hex.decode(Commons.BC_SALT.getBytes()), 128);
            cipherParameters = pGen.generateDerivedParameters(192, 64);
    	}
    	return cipherParameters;
    }
}
