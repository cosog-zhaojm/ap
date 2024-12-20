package com.cosog.model.calculate;

import java.io.Serializable;
import java.util.List;

public class UserInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Integer userNo;
	
	private Integer userOrgid;
	
	private String userId;
	private String userName;
	private String userPwd;
	
	private String userInEmail;
	private String userPhone;
	
	private Integer userQuickLogin;
	private Integer userEnable;
	private Integer receiveSMS;
	private Integer receiveMail;
	
	private Integer userType;
	private String roleName;
	private Integer roleLevel;
	private Integer roleShowLevel;
	
	private Integer language;
	private String languageName;
	
	private List<Integer> orgChildrenNode;
	private List<Integer> deviceTypeChildrenNode;
	public UserInfo() {
		super();
	}
	public UserInfo(Integer userNo, Integer userOrgid, String userId, String userName, String userPwd,
			String userInEmail, String userPhone, Integer userQuickLogin, Integer userEnable, Integer receiveSMS,
			Integer receiveMail, Integer userType, String roleName, Integer roleLevel,
			Integer roleShowLevel,Integer language,String languageName) {
		super();
		this.userNo = userNo;
		this.userOrgid = userOrgid;
		this.userId = userId;
		this.userName = userName;
		this.userPwd = userPwd;
		this.userInEmail = userInEmail;
		this.userPhone = userPhone;
		this.userQuickLogin = userQuickLogin;
		this.userEnable = userEnable;
		this.receiveSMS = receiveSMS;
		this.receiveMail = receiveMail;
		this.userType = userType;
		this.roleName = roleName;
		this.roleLevel = roleLevel;
		this.roleShowLevel = roleShowLevel;
		this.language = language;
		this.languageName = languageName;
	}
	public Integer getUserNo() {
		return userNo;
	}
	public void setUserNo(Integer userNo) {
		this.userNo = userNo;
	}
	public Integer getUserOrgid() {
		return userOrgid;
	}
	public void setUserOrgid(Integer userOrgid) {
		this.userOrgid = userOrgid;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getUserPwd() {
		return userPwd;
	}
	public void setUserPwd(String userPwd) {
		this.userPwd = userPwd;
	}
	public String getUserInEmail() {
		return userInEmail;
	}
	public void setUserInEmail(String userInEmail) {
		this.userInEmail = userInEmail;
	}
	public String getUserPhone() {
		return userPhone;
	}
	public void setUserPhone(String userPhone) {
		this.userPhone = userPhone;
	}
	public Integer getUserQuickLogin() {
		return userQuickLogin;
	}
	public void setUserQuickLogin(Integer userQuickLogin) {
		this.userQuickLogin = userQuickLogin;
	}
	public Integer getUserEnable() {
		return userEnable;
	}
	public void setUserEnable(Integer userEnable) {
		this.userEnable = userEnable;
	}
	public Integer getReceiveSMS() {
		return receiveSMS;
	}
	public void setReceiveSMS(Integer receiveSMS) {
		this.receiveSMS = receiveSMS;
	}
	public Integer getReceiveMail() {
		return receiveMail;
	}
	public void setReceiveMail(Integer receiveMail) {
		this.receiveMail = receiveMail;
	}
	public Integer getUserType() {
		return userType;
	}
	public void setUserType(Integer userType) {
		this.userType = userType;
	}
	public String getRoleName() {
		return roleName;
	}
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
	public Integer getRoleLevel() {
		return roleLevel;
	}
	public void setRoleLevel(Integer roleLevel) {
		this.roleLevel = roleLevel;
	}
	public Integer getRoleShowLevel() {
		return roleShowLevel;
	}
	public void setRoleShowLevel(Integer roleShowLevel) {
		this.roleShowLevel = roleShowLevel;
	}
	public Integer getLanguage() {
		return language;
	}
	public void setLanguage(Integer language) {
		this.language = language;
	}
	public String getLanguageName() {
		return languageName;
	}
	public void setLanguageName(String languageName) {
		this.languageName = languageName;
	}
	public List<Integer> getOrgChildrenNode() {
		return orgChildrenNode;
	}
	public void setOrgChildrenNode(List<Integer> orgChildrenNode) {
		this.orgChildrenNode = orgChildrenNode;
	}
	public List<Integer> getDeviceTypeChildrenNode() {
		return deviceTypeChildrenNode;
	}
	public void setDeviceTypeChildrenNode(List<Integer> deviceTypeChildrenNode) {
		this.deviceTypeChildrenNode = deviceTypeChildrenNode;
	}
	
}
