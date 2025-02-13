<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	String path = request.getContextPath();
%>
<%  
	String flag="";
	String name="";
	String password="";
	
	
	String viewProjectName="";
	if(session.getAttribute("viewProjectName")!=null){
		viewProjectName=(String)session.getAttribute("viewProjectName");
	}
	String favicon="";
	if(session.getAttribute("favicon")!=null){
		favicon=(String)session.getAttribute("favicon");
		favicon=favicon.substring(favicon.indexOf("/"),favicon.length());
	}
	String loginCSS="";
	if(session.getAttribute("loginCSS")!=null){
		loginCSS=(String)session.getAttribute("loginCSS");
		loginCSS=loginCSS.substring(loginCSS.indexOf("/"),loginCSS.length());
	}
	boolean showLogo=false;
	if(session.getAttribute("showLogo")!=null){
		showLogo=(boolean)session.getAttribute("showLogo");
	}
	
	String oemStaticResourceTimestamp="";
	if(session.getAttribute("oemStaticResourceTimestamp")!=null){
		oemStaticResourceTimestamp=(String)session.getAttribute("oemStaticResourceTimestamp");
	}
	
	String otherStaticResourceTimestamp="";
	if(session.getAttribute("otherStaticResourceTimestamp")!=null){
		otherStaticResourceTimestamp=(String)session.getAttribute("otherStaticResourceTimestamp");
	}
	try{
		Cookie[] cookies=request.getCookies();
		System.out.println(cookies.length);
		if(cookies!=null){
			for(int i=0;i<cookies.length;i++){
				System.out.println(cookies[i].getName());
				if("cookieuser".equals(cookies[i].getName()+":"+cookies[i].getValue())){
					String value=cookies[i].getValue();
					if(value!=null&&!"".equals(value)){
						name=cookies[i].getValue().split("-")[0];
						if(cookies[i].getValue().split("-")[1]!=null && !cookies[i].getValue().split("-")[1].equals("null")){
							password=cookies[i].getValue().split("-")[1];
							flag="1";
						}
					}
				}
				request.setAttribute("name", name);
				request.setAttribute("password", password);
			}
		}
		
	}catch(Exception e){
		e.printStackTrace();
	}
%>

<!DOCTYPE html>
<html>

<head>
	<meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <meta charset="UTF-8">
    <!--<fmt:setBundle basename="config/messages" />-->
    <!--<script type="text/javascript" src="<%=path%>/app/locale.js?timestamp=202303300850"></script>-->
    <meta name="description" content="">
    <meta name="keywords" content="">
    <meta name="viewport" content="width=device-width,initial-scale=1.0,minimum-scale=1.0,maximum-scale=1.0,user-scalable=no,minimal-ui">
    <meta name="renderer" content="webkit">
    <!--默认360极速模式-->
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta http-equiv="Content-Type" content="text/html" />
    <meta http-equiv="pragma" content="no-cache" />
    <meta http-equiv="cache-control" content="no-cache,must-revalidate" />
    <meta http-equiv="expires" content="0" />
    <title><%=viewProjectName%></title>
    <!-- 链接外部图标，如：中石油、中石化 -->
    <%if(showLogo){ %>
	<link rel="Bookmark" href="<%=path+favicon%>?timestamp=<%=oemStaticResourceTimestamp%>>" />
	<link rel="icon" href="<%=path+favicon%>?timestamp=<%=oemStaticResourceTimestamp%>" type="image/x-icon" />
	<link rel="shortcut icon" href="<%=path+favicon%>?timestamp=<%=oemStaticResourceTimestamp%>" type="image/x-icon" />
	<%} %>
    <!-- 链接css -->
    <link rel="stylesheet" href="<%=path%>/scripts/bootstrap/css/bootstrap.min.css?timestamp=<%=otherStaticResourceTimestamp%>" type="text/css" />
    <link rel="stylesheet" href="<%=path%>/scripts/bootstrap/css/bootstrap-select.min.css?timestamp=<%=otherStaticResourceTimestamp%>"" type="text/css" />
    <link rel="stylesheet" href="<%=path%>/scripts/bootstrap/css/site.css?timestamp=<%=otherStaticResourceTimestamp%>" type="text/css" />
    <link rel="stylesheet" href="<%=path+loginCSS%>?timestamp=<%=oemStaticResourceTimestamp%>" type="text/css"/>

    <script type="text/javascript" src="<%=path%>/scripts/jquery/jquery-3.6.1.min.js?timestamp=<%=otherStaticResourceTimestamp%>"></script>
    <script type="text/javascript" src="<%=path%>/scripts/bootstrap/js/bootstrap.min.js?timestamp=<%=otherStaticResourceTimestamp%>"></script>
    <script type="text/javascript" src="<%=path%>/scripts/bootstrap/js/bootstrap-select.min.js?timestamp=<%=otherStaticResourceTimestamp%>"></script>
	<script>
	var context='<%=path%>'; 
	
	var oem = ${configFile}.ap.oem;
	var loginBackgroundImage=oem.loginBackgroundImage;
	loginBackgroundImage=context+loginBackgroundImage.substring(loginBackgroundImage.indexOf("/"),loginBackgroundImage.length);
	var loginLanguageResource=${loginLanguageResource};
	
	$(function () {
		initDisplayInformation();
		var getUserListURL="<%=path%>/userLoginManagerController/getUserList";
		$.ajax({
			url: getUserListURL,
			cache:false,
			success:function(data){
				//alert(data);
				var optionString="<option value=\'\' data-password=\'\' selected>"+loginLanguageResource.myself+"</option>";
				optionString+="<option data-divider='true'></option>";
				for(var i=0;i<data.length;i++){
					optionString += "<option value=\'"+ data[i].useraccount +"\' data-password=\'"+data[i].userpwd+"\'>" + data[i].username + "</option>";
				}
				var userSelectpicker=document.getElementById("userSelectpicker");
				$("#userSelectpicker").html(optionString);
				$("#userSelectpicker").selectpicker('refresh');
			}
		});
	});
	
	function initDisplayInformation(){
		
		$("#login_userlogin").html(loginLanguageResource.userLogin);
		$("#login_loginInfo").html(loginLanguageResource.projectProfile);
		$("#login_title").html(loginLanguageResource.projectName);
		$("#userSelectpicker").html(loginLanguageResource.myself);
		$("#userId").attr("placeholder", loginLanguageResource.enterUserName);
		$("#userPwd").attr("placeholder", loginLanguageResource.enterPassword);
		$("#login_rememberpassword").html(loginLanguageResource.rememberPassword);
		$("#login_forgerpassword").html(loginLanguageResource.forgerPassword);
		$("#login_contact").html(loginLanguageResource.contact);
		$("#login_loginButton").html(loginLanguageResource.login);
		$("#login_copy").html(loginLanguageResource.copy);
		$("#login_link").text(loginLanguageResource.linkshow);
		$('#login_link').attr('href',loginLanguageResource.linkaddress);
		
		//$(".page-login:before").css("background-image", "url("+loginBackgroundImage+")");
		
		$('<style>.page-login:before{background-image:url('+loginBackgroundImage+');}</style>').appendTo('head');
		
		
		var userLoginName=localStorage.getItem("userLoginName");
	    var userLoginPassword=localStorage.getItem("userLoginPassword");
	    var userLoginRememberPassword=localStorage.getItem("userLoginRememberPassword");
	    
	    if(userLoginRememberPassword==1){
	    	$("#userId").val(userLoginName);
	    	$("#userPwd").val(userLoginPassword);
	    	$("#flag").prop("checked",true);
	    }else{
	    	$("#userId").val('');
	    	$("#userLoginPassword").val('');
	    	$("#flag").prop("checked",false);
	    }
	}
	
	function userSelectpickerChange(obj){
		if(obj[obj.selectedIndex].value!=""){
			verifyAutoFn(obj[obj.selectedIndex].value,obj[obj.selectedIndex].dataset.password);
		}else{
			$("#userId").focus();
		}
	}
	// 登录
	function loginFn(){
		var get_name=$("#userId").val();
		var get_pwd=$("#userPwd").val();
		if(null!=get_name&&get_name!=""&&null!=get_pwd&&get_pwd!=""){
			$("#errorName").html("<font style='color:#666666'>"+loginLanguageResource.logining+"</font>");
			$("#errorPwd").html("");
			verifyFn();
		}else if(null==get_name||get_name==""){
			error_NameFn();
		}else if(null==get_pwd||get_pwd==""){
			error_PwdFn();
	   	} 
	 }
	
	username="";
	// 真实登录进入前台
	function verifyFn(){
		var getTime=new Date();
		var getSeconds=getTime.getMinutes()+getTime.getSeconds();
		var getLogin_=$("#userId").val();
		var userLoginPassword=$("#userPwd").val();
	   	var userId_ = encodeURI(encodeURI(getLogin_));
		var userPwd_ = encodeURI(encodeURI(userLoginPassword));
		var flag_=0;
		if($("#flag").prop("checked")){
			flag_=1;
		}
		var paramObj ="?&time="+getSeconds+"&userId=" + userId_ + "&userPwd=" + userPwd_+"&flag=" + flag_;
		var url=context + '/userLoginManagerController/userLogin';
	   // jquery Ajax请求事件
		$.ajax({
			url: url,
			cache:false,
			data: {
				time: getSeconds,
				userId: userId_,
				userPwd: userPwd_,
				flag: flag_
			},
			success:function(data){
			    username=getLogin_;
			    localStorage.setItem("userLoginName",getLogin_);
			    localStorage.setItem("userLoginPassword",userLoginPassword);
			    localStorage.setItem("userLoginRememberPassword",flag_);
			    
			    
				callback(data,username);
			}
		});
		return false;
	 };
	 //自动登录
	 function verifyAutoFn(selectUsername,selectUserpwd){
		 	$("#errorName").html("<font style='color:#666666'>"+loginLanguageResource.logining+"</font>");
			$("#errorPwd").html("");
			var getTime=new Date();
			var getSeconds=getTime.getMinutes()+getTime.getSeconds();
		   	var userId_ = encodeURI(encodeURI(selectUsername));
			var userPwd_ = encodeURI(encodeURI(selectUserpwd));
			var flag_=0;
			var autoLogin=1;
			//if($("#flag").prop("checked")){
			//	flag_=1;
			//}
			var paramObj ="?&time="+getSeconds+"&userId=" + userId_ + "&userPwd=" + userPwd_+"&flag=" + flag_+"&autoLogin="+autoLogin;	
			var url="<%=path%>/userLoginManagerController/userLogin"+paramObj;
		   // jquery Ajax请求事件
			$.ajax({
				url: url,
				cache:false,
				success:function(data){
				    username=selectUsername;
					callback(data,username);
				}
			});
			return false;
	};
	 
	// 登录前台的返调函数处理
	 function callback(data,username) {
		var obj;
		var resultObj = $("#errorName");
		if(data!=""||data!=null){
			obj= eval("(" + data + ")");
		}else{
			window.location.href = "<%=path%>/errorinfo.html";
		}
		if (obj.flag == "normal") {
			//$.cookie("username", username, { expires: 7 });
			window.location.href = "<%=path%>/home";
		} else if (obj.flag == "admin") {
			resultObj.html(obj.msg);
			window.location.href = "<%=path%>/toBackLogin";
		} else {
			resultObj.html(obj.msg);
		}
		return false;
	}
	// 登录账号处理
	 function error_NameFn(){
	     $("#userId").focus();
	     $("#errorName").html(loginLanguageResource.enterUserName);
	     return false;
	 }
	 
	 // 登录密码处理
	 function error_PwdFn(){
	     $("#userPwd").focus();
	     $("#errorName").html(loginLanguageResource.enterPassword); 
	     return false;
	 }
	</script>
</head>

<body class="page-login layout-full page-dark">
    <div class="page height-full">
        <div class="page-content height-full">
            <div class="page-brand-info vertical-align animation-slide-left hidden-xs">
                <div class="page-brand vertical-align-middle">
                    <ul class="list-icons hidden-sm">
                        <li style="text-indent:35px" >
                        	  <span  id= "login_loginInfo"></span>
                        </li>
                    </ul>
                    <div class="hidden-sm"></div>
                </div>
            </div>
            <div class="page-login-main animation-fade">
                <div class="vertical-align">
                    <div class="vertical-align-middle">
                        <div class="visible-xs text-center">
                            Agile
                        </div>
                        <h3 class="hidden-xs" style="margin-bottom: 20px;"><span  id= "login_userlogin"></span> </h3>
                        <h4 class="hidden-xs" style="margin-bottom: 0px;"><span  id= "login_title"></span></h4>
                        <!-- <p class="hidden-xs"></p> -->
                        <form id="frmlogin" class="login-form fv-form fv-form-bootstrap" method="post">
							<div class="form-group " >
								<select  class="selectpicker form-control" id="userSelectpicker" onchange="userSelectpickerChange(this)">
									<!-- <option ><span  id= "login_myself"></span></option> -->
								</select>
							</div>
                            <div class="form-group">
                                <input type="text" id="userId" class="form-control"  required="" value="">
                            </div>
                            <div class="form-group">
                                <input type="password" id="userPwd" class="form-control" required="" value="" onkeypress="if(event.keyCode==13){loginFn();}">
                            </div>
                            <div class="form-group clearfix">
        						<div class="checkbox-custom checkbox-inline checkbox-primary pull-left">
            						
					  				<input type="checkbox" id="flag" name="flag" value="0">
            						<label for="remember" id="login_rememberpassword"></label>
        						</div>
        						<a class="pull-right collapsed" data-toggle="collapse" href="#forgetPassword" aria-expanded="false" aria-controls="forgetPassword" id="login_forgerpassword">
        						</a>
    						</div>
    						<div class="collapse" id="forgetPassword" aria-expanded="false" style="height: 0px;">
                            	<div class="alert alert-warning alert-dismissible" role="alert" id="login_contact">
                            	</div>
                        	</div>
                            <button type="button" class="btn btn-primary btn-block margin-top-30" id="login_loginButton" onclick="loginFn();"></button>
                            <span id="errorName" class="errorMsg"></span>
                        </form>
                    </div>
                </div>
                <footer class="page-copyright">
                    <p><span id="login_copy"></span><a target='_blank' id='login_link'></a></p>
                </footer>
            </div>
        </div>
    </div>

</body>

</html>