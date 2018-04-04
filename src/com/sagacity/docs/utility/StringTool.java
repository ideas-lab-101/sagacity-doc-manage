package com.sagacity.docs.utility;


import com.jfinal.kit.StrKit;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTool extends StrKit {
	public static final String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	public static final String letterChar = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	public static final String numberChar = "0123456789";

	/**
	 * 验证邮箱
	 * @param email
	 * @return
	 */
	public static boolean checkEmail(String email){
		boolean flag = false;
		try{
			String check = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
			Pattern regex = Pattern.compile(check);
			Matcher matcher = regex.matcher(email);
			flag = matcher.matches();
		}catch(Exception e){
			flag = false;
		}
		return flag;
	}

	/**
	 * 验证手机号码
	 * @param mobileNumber
	 * @return
	 */
	public static boolean checkMobileNumber(String mobileNumber){
		boolean flag = false;
		try{
			Pattern regex = Pattern.compile("^(((13[0-9])|(15([0-3]|[5-9]))|(17([6-8]))|(18[0-3,5-9]))\\d{8})|(0\\d{2}-\\d{8})|(0\\d{3}-\\d{7})$");
			Matcher matcher = regex.matcher(mobileNumber);
			flag = matcher.matches();
		}catch(Exception e){
			flag = false;
		}
		return flag;
	}


	public static String generateMixString(int length) 
	{

		StringBuffer sb = new StringBuffer();
		
		Random random = new Random();
		
		for (int i = 0; i < length; i++) {
		
		sb.append(allChar.charAt(random.nextInt(letterChar.length())));
		
		}
		
		return sb.toString();	

	}

	public static String generateNumberString(int length) 
	{

		StringBuffer sb = new StringBuffer();
		
		Random random = new Random();
		
		for (int i = 0; i < length; i++) {
		
		sb.append(numberChar.charAt(random.nextInt(numberChar.length())));
		
		}
		
		return sb.toString();	

	}
	public static String generateUpperString(int length) {

		return generateMixString(length).toUpperCase();

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("generateMixString(6)");
		for (int i=0 ;i<10;i++)
		{
		System.out.println(generateUpperString(6));
		}
		
		System.out.println("generateNumberString(6)");
		for (int i=0 ;i<10;i++)
		{
		System.out.println(generateNumberString(6));
		}		


	}

}