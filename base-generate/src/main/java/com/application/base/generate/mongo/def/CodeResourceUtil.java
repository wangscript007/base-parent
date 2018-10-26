package com.application.base.generate.mongo.def;

import java.util.ResourceBundle;

/**
 * 
 * 获得数据资源
 * 
 * @author bruce.
 * 
 */

public class CodeResourceUtil {

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("config/mongo_generate");
	
	public static String DIVER_NAME = "com.mongdb.jdbc.Driver";

	public static String URL = "127.0.0.1:27017";

	public static String USERNAME = "admin";

	public static String PASSWORD = "admin";

	public static String DATABASE_NAME = "data";

	public static String DATABASE_TYPE = "mongo";

	public static String web_root_package = "WebRoot";

	public static String source_root_package = "src";

	public static String bussiPackage = "sun";

	public static String bussiPackageUrl = "sun";

	public static String entity_package = "entity";

	public static String page_package = "page";

	public static String ENTITY_URL;

	public static String PAGE_URL;

	public static String ENTITY_URL_INX;

	public static String PAGE_URL_INX;

	public static String TEMPLATEPATH;

	public static String CODEPATH;

	public static String JSPPATH;

	public static String JAVABASE_GENERATE_TABLE_ID;

	public static String JAVABASE_GENERATE_UI_FILTER_FIELDS;

	public static String SYSTEM_ENCODING;

	/**
	 * 初始化数字操作。 
	 */
	static {
		
		DIVER_NAME = getDiverName();
		
		URL = getUrl();
		
		USERNAME = getUserName();
		
		PASSWORD = getPassword();
		
		DATABASE_NAME = getDatabaseName();

		SYSTEM_ENCODING = getSystemEncoding();
		
		TEMPLATEPATH = getTemplatePath();
		
		source_root_package = getSourceRootPackage();
		
		web_root_package = getWebRootPackage();
		
		bussiPackage = getBussiPackage();
		
		bussiPackageUrl = bussiPackage.replace(".", "/");

		JAVABASE_GENERATE_TABLE_ID = getJavaBaseGenerateTableId();

		source_root_package = source_root_package.replace(".", "/");
		
		web_root_package = web_root_package.replace(".", "/");

		ENTITY_URL = source_root_package + "/" + bussiPackageUrl + "/"+ entity_package + "/";

		PAGE_URL = source_root_package + "/" + bussiPackageUrl + "/"+ page_package + "/";

		ENTITY_URL_INX = bussiPackage + "." + entity_package + ".";

		PAGE_URL_INX = bussiPackage + "." + page_package + ".";

		CODEPATH = source_root_package + "/" + bussiPackageUrl + "/";

		JSPPATH = web_root_package + "/" + bussiPackageUrl + "/";
	}


	public static final String getDiverName() {
		return BUNDLE.getString("mongo.diver_name");
	}

	public static final String getUrl() {
		return BUNDLE.getString("mongo.url");
	}

	public static final String getUserName() {
		return BUNDLE.getString("mongo.username");
	}

	public static final String getPassword() {
		return BUNDLE.getString("mongo.password");
	}

	public static final String getDatabaseName() {
		return BUNDLE.getString("mongo.database_name");
	}

	private static String getBussiPackage() {
		return BUNDLE.getString("bussi_package");
	}

	public static final String getEntityPackage() {
		return BUNDLE.getString("entity_package");
	}

	public static final String getPagePackage() {
		return BUNDLE.getString("page_package");
	}

	public static final String getTemplatePath() {
		return BUNDLE.getString("templatepath");
	}

	public static final String getSourceRootPackage() {
		return BUNDLE.getString("source_root_package");
	}

	public static final String getWebRootPackage() {
		return BUNDLE.getString("webroot_package");
	}

	public static final String getSystemEncoding() {
		return BUNDLE.getString("system_encoding");
	}

	public static final String getJavaBaseGenerateTableId() {
		return BUNDLE.getString("mongo_generate_table_id");
	}
	
}
