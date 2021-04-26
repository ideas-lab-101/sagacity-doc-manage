package com.sagacity.docs.model.user;

import com.jfinal.ext.plugin.tablebind.TableBind;
import com.jfinal.plugin.activerecord.Model;

@TableBind(tableName="user_profile",pkName="profile_id")
public class UserProfile extends Model<UserProfile> {
	private static final long serialVersionUID = 1L;
	public final static UserProfile dao = new UserProfile();

	public UserProfile findById(String userId){
		UserProfile up = UserProfile.dao.findFirst("select * from user_profile where user_id=?", userId);
		return  up;
	}
	
}
