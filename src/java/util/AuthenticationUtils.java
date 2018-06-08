/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;
/**
 *
 * @author shubhamv
 */
public class AuthenticationUtils {
    /**
	 * Returns SHA-256 encoded string
	 * @param password - the string to be encoded
	 * @return SHA-256 encoded string
	 * @throws UnsupportedEncodingException if UTF-8 is not supported by the system
	 * @throws NoSuchAlgorithmException if SHA-256 is not supported by the system
	 */
	public static String encodeSHA256(String password) 
			throws UnsupportedEncodingException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(password.getBytes("UTF-8"));
        byte[] digest = md.digest();
        return DatatypeConverter.printBase64Binary(digest).toString();
    }
}
