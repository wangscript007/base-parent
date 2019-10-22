package com.application.base.core.impl;

import com.application.base.conts.DataConstant;
import com.application.base.util.sql.PkProvider;
import com.application.base.util.xml.ColumnInfo;
import com.application.base.util.xml.ItemInfo;
import com.application.base.util.xml.TableInfo;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author : 孤狼
 * @NAME: JsonDataParser
 * @DESC: 处理json
 **/
@Component
public class JsonDataParser extends CommonDataParser {
	
	private Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	/**
	 * 为空判断
	 */
	private final String  jsonEmpty = "[]";
	
	public LinkedList<String> getInsertSql(String jsonContent, String companyId, TableInfo tableInfo){
		return getInsertSql(jsonContent,companyId,tableInfo,null);
	}
	
	public LinkedList<String> getInsertSql(String jsonContent, String companyId, TableInfo tableInfo, PkProvider provider){
		if (StringUtils.isEmpty(jsonContent) || jsonContent.equals(jsonEmpty)){
			logger.info("传入的json字符串是:{}",jsonContent);
			return null;
		}
		LinkedList<String> sqls = new LinkedList<>();
		List<Object> dataList = JsonPath.read(jsonContent, tableInfo.getPath());
		for (Object object : dataList) {
			LinkedHashMap<String,Object> dataMap = (LinkedHashMap<String, Object>) object;
			sqls.addAll(getSql(dataMap,jsonContent,tableInfo,companyId,provider));
		}
		return sqls;
	}
	
	/**
	 * 通过json串获得建表的sql.
	 * @param jsonContent
	 * @param tableInfo
	 * @return
	 */
	public LinkedList<String> getCreateTableSql(String jsonContent,TableInfo tableInfo) {
		if (tableInfo==null || StringUtils.isEmpty(jsonContent) || jsonContent.equals(jsonEmpty)){
			logger.info("传入的json字符串是:{}",jsonContent);
			return null;
		}
		LinkedList<String> finalSqls = new LinkedList<>();
		finalSqls.add(deleteTableSql(tableInfo));
		
		//处理列信息.
		StringBuffer buffer = new StringBuffer();
		LinkedList<String> hearders = getTableHearderInfo(tableInfo, buffer);
		String primDesc = hearders.get(0);
		LinkedList<ColumnInfo> columnInfos = tableInfo.getColumns();
		if (columnInfos==null || columnInfos.size()==0){
			//列信息.
			LinkedHashSet<String> delColumns = new LinkedHashSet<>();
			//有text类型的字段.
			String[] textFields = tableInfo.getTextField();
			if (textFields!=null && textFields.length>0){
				for (String text : textFields) {
					if (StringUtils.isNotBlank(text)){
						delColumns.add(text);
						buffer.append("\t"+text).append(" text comment '' ,\n");
					}
				}
			}
			//有子项的情况.
			String[] deleteItems = tableInfo.getDeleteItem();
			if (deleteItems!=null && deleteItems.length>0){
				for (String item : deleteItems) {
					if (StringUtils.isNotBlank(item)){
						delColumns.add(item);
					}
				}
			}
			List<Object> dataList = JsonPath.read(jsonContent, tableInfo.getPath());
			//数据的所有列.
			//以最多列的数据为准创建表结构.
			LinkedHashMap<String,Object> dataMap = getDataMap(dataList);
			Set<String> keys = dataMap.keySet();
			//天眼查接口:http://www.open.com/services/v4/open/patents 返回 applicantname 和 applicantName 同列.
			LinkedHashSet<String> allColumns = new LinkedHashSet<>();
			for (String column:keys) {
				allColumns.add(column.toLowerCase());
			}
			for (String column:keys) {
				if (!delColumns.contains(column)){
					if (allColumns.contains(column.toLowerCase())){
						allColumns.remove(column.toLowerCase());
						buffer.append("\t"+column).append(" varchar("+tableInfo.getColumnLen()+") default '' comment '',\n");
					}
				}
			}
		}else{
			List<ColumnInfo> columns = tableInfo.getColumns();
			for (ColumnInfo info : columns ) {
				if (StringUtils.isNotBlank(info.getOwner())){
					buffer.append("\t"+info.getOwner());
				}else{
					buffer.append("\t"+info.getName());
				}
				if (StringUtils.isNotBlank(info.getType())){
					buffer.append(" "+info.getType()+"("+info.getLength()+")");
				}else{
					buffer.append(" varchar(200) ");
				}
				if (info.getRequired().equalsIgnoreCase(DataConstant.FLAG_VALUE)){
					buffer.append(" not null ");
				}
				buffer.append(" default '"+info.getDefaultValue()+"' comment '"+info.getComments()+"',\n");
			}
		}
		buffer.append("\t"+primDesc);
		if (StringUtils.isNotEmpty(tableInfo.getComments())){
			buffer.append("\n) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '"+tableInfo.getComments()+"';");
		}else{
			buffer.append("\n) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '"+tableInfo.getTableName()+"表信息';");
		}
		finalSqls.add(buffer.toString());
		
		//处理字表的情况
		if (tableInfo.isItems()){
			finalSqls.addAll(getCreateItemTableSql(jsonContent,tableInfo.getItemInfos(),hearders.get(1)));
		}
		return  finalSqls;
	}
	
	/**
	 * 获取列操作.
	 * @param dataList
	 * @return
	 */
	private LinkedHashMap<String, Object> getDataMap(List<Object> dataList) {
		LinkedHashMap<String,Object> dataMap = (LinkedHashMap<String, Object>) dataList.get(0);
		for (Object object : dataList) {
			LinkedHashMap<String,Object> tmpMap = (LinkedHashMap<String, Object>) object;
			if (tmpMap.size()>dataMap.size()){
				dataMap = tmpMap;
			}
		}
		return dataMap;
	}
	
	/**
	 * 生成子项的sql.
	 * @param jsonContent
	 * @param itemInfos
	 * @param mainPk
	 * @return
	 */
	private LinkedList<String> getCreateItemTableSql(String jsonContent, LinkedList<ItemInfo> itemInfos, String mainPk) {
		LinkedList<String> itemSqls = new LinkedList<>();
		for (ItemInfo itemInfo : itemInfos) {
			//处理列信息.
			StringBuffer buffer = new StringBuffer();
			String primDesc = getTableItemHearderInfo(itemInfo, buffer,mainPk);
			LinkedList<ColumnInfo> columnInfos = itemInfo.getColumns();
			if (columnInfos==null || columnInfos.size()==0){
				//列信息.
				LinkedList<String> delColumns = new LinkedList<>();
				//有text类型的字段.
				String[] textFields = itemInfo.getTextField();
				if (textFields!=null && textFields.length>0){
					for (String text : textFields) {
						if (StringUtils.isNotBlank(text)){
							delColumns.add(text);
							buffer.append("\t"+text).append(" text comment '' ,\n");
						}
					}
				}
				List<Object> dataList = JsonPath.read(jsonContent, itemInfo.getPath());
				//以最多列的数据为准创建表结构.
				LinkedHashMap<String,Object> dataMap = getDataMap(dataList);
				Set<String> keys = dataMap.keySet();
				for (String column:keys) {
					if (!delColumns.contains(column)){
						buffer.append("\t"+column).append(" varchar("+itemInfo.getColumnLen()+") default '' comment '',\n");
					}
				}
			}else{
				//有列的情况
				List<ColumnInfo> columns = itemInfo.getColumns();
				for (ColumnInfo info : columns ) {
					if (StringUtils.isNotBlank(info.getOwner())){
						buffer.append("\t"+info.getOwner());
					}else{
						buffer.append("\t"+info.getName());
					}
					if (StringUtils.isNotBlank(info.getType())){
						buffer.append(" "+info.getType()+"("+info.getLength()+")");
					}else{
						buffer.append(" varchar(200) ");
					}
					if (info.getRequired().equalsIgnoreCase(DataConstant.FLAG_VALUE)){
						buffer.append(" not null ");
					}
					buffer.append(" default '"+info.getDefaultValue()+"' comment '"+info.getComments()+"',\n");
				}
			}
			buffer.append("\t"+primDesc);
			if (StringUtils.isNotEmpty(itemInfo.getComments())){
				buffer.append("\n) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '"+itemInfo.getComments()+"';");
			}else{
				buffer.append("\n) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '"+itemInfo.getTableName()+"表信息';");
			}
			//删除再添加.
			itemSqls.add(deleteTableSql(itemInfo));
			itemSqls.add(buffer.toString());
		}
		return itemSqls;
	}
	
	/**
	 * 删除表结构的语句
	 * @param tableInfo
	 * @return
	 */
	private String deleteTableSql(TableInfo tableInfo) {
		return "drop table if exists "+tableInfo.getTableName()+";";
	}
	
	/**
	 * 删除表结构的语句
	 * @param itemInfo
	 * @return
	 */
	private String deleteTableSql(ItemInfo itemInfo) {
		return "drop table if exists "+itemInfo.getTableName()+";";
	}
	
	/**
	 * 获得sql.
	 * @param dataMap
	 * @param tableInfo
	 * @param companyId
	 * @return
	 */
	private LinkedList<String> getSql(LinkedHashMap<String, Object> dataMap,String jsonContent,TableInfo tableInfo,String companyId,PkProvider provider) {
		LinkedList<String> sqls = new LinkedList<>();
		
		StringBuffer buffer = new StringBuffer();
		//INSERT INTO table_name (列1, 列2,...) VALUES (值1, 值2,....)
		buffer.append("insert into "+tableInfo.getTableName()+" (");
		if (!tableInfo.getAutoPk().equalsIgnoreCase(DataConstant.FLAG_VALUE)){
			buffer.append(tableInfo.getPrimKey()+",");
		}
		buffer.append("companyId,");
		LinkedList<ColumnInfo> columns = builderHeaderColumns(tableInfo, buffer,dataMap);
		Object pkValue="";
		if (!tableInfo.getAutoPk().equalsIgnoreCase(DataConstant.FLAG_VALUE) && provider!=null){
			String primType = tableInfo.getPrimType();
			if (primType.equalsIgnoreCase("Integer")) {
				if (StringUtils.isEmpty(provider.getIntPrimKey()+"")){
					pkValue = provider.defaultIntPk();
					buffer.append(pkValue+",");
				}else{
					pkValue = provider.getIntPrimKey();
					buffer.append(pkValue+",");
				}
			} else if (primType.equalsIgnoreCase("String")) {
				if (StringUtils.isEmpty(provider.getStrPrimKey())){
					pkValue = provider.defaultStrPk();
					buffer.append("'"+pkValue+"',");
				}else{
					pkValue = provider.getStrPrimKey();
					buffer.append("'"+pkValue+"',");
				}
			}
		}
		buffer.append("'"+companyId+"',");
		builderHeaderValues(dataMap, buffer, columns);
		String sql=buffer.toString();
		sqls.add(sql);
		//处理子项的操作.
		if (tableInfo.isItems()){
			sqls.addAll(getItemSql(jsonContent,tableInfo.getItemInfos(),companyId,pkValue,provider));
		}
		return sqls;
	}
	
	/**
	 * 获得sql.
	 * @param dataMap
	 * @param itemInfo
	 * @param companyId
	 * @return
	 */
	private LinkedList<String> getItemsSql(LinkedHashMap<String, Object> dataMap,ItemInfo itemInfo,String companyId,Object pkValue,PkProvider provider) {
		LinkedList<String> sqls = new LinkedList<>();
		
		StringBuffer buffer = new StringBuffer();
		//INSERT INTO table_name (列1, 列2,...) VALUES (值1, 值2,....)
		buffer.append("insert into "+itemInfo.getTableName()+" (");
		if (!itemInfo.getAutoPk().equalsIgnoreCase(DataConstant.FLAG_VALUE)){
			buffer.append(itemInfo.getPrimKey()+",");
		}
		buffer.append("companyId,");
		LinkedList<ColumnInfo> columns = builderItemColumns(itemInfo,buffer,dataMap);
		if (!itemInfo.getAutoPk().equalsIgnoreCase(DataConstant.FLAG_VALUE) && provider!=null){
			String primType = itemInfo.getPrimType();
			if (primType.equalsIgnoreCase("Integer")) {
				if (StringUtils.isEmpty(provider.getIntPrimKey()+"")){
					buffer.append(provider.defaultIntPk()+",");
				}else{
					buffer.append("'"+provider.getIntPrimKey()+"',");
				}
			} else if (primType.equalsIgnoreCase("String")) {
				if (StringUtils.isEmpty(provider.getStrPrimKey())){
					buffer.append("'"+provider.defaultStrPk()+"',");
				}else{
					buffer.append("'"+provider.getStrPrimKey()+"',");
				}
			}
		}
		buffer.append("'"+companyId+"',");
		buffer.append("'"+pkValue+"',");
		builderItemValues(dataMap,buffer,columns);
		String sql=buffer.toString();
		sqls.add(sql);
		return sqls;
	}
	
	
	/**
	 * 获得子项的sql
	 * @param jsonContent
	 * @param itemInfos
	 * @param companyId
	 * @param pkValue
	 * @return
	 */
	private LinkedList<String> getItemSql(String jsonContent, LinkedList<ItemInfo> itemInfos, String companyId, Object pkValue,PkProvider provider) {
		LinkedList<String> sqls = new LinkedList<>();
		StringBuffer buffer = new StringBuffer();
		for (ItemInfo itemInfo : itemInfos) {
			List<Object> dataList = JsonPath.read(jsonContent, itemInfo.getPath());
			for (Object object : dataList) {
				LinkedHashMap<String,Object> dataMap = (LinkedHashMap<String, Object>) object;
				sqls.addAll(getItemsSql(dataMap,itemInfo,companyId,pkValue,provider));
			}
		}
		return sqls;
	}
	
	/**
	 * 获得列信息
	 * @param tableInfo
	 * @param buffer
	 * @return
	 */
	private LinkedList<ColumnInfo> builderHeaderColumns(TableInfo tableInfo, StringBuffer buffer, LinkedHashMap<String, Object> dataMap) {
		LinkedList<ColumnInfo> columns = tableInfo.getColumns();
		if (columns==null || columns.size()==0){
			columns = new LinkedList<>();
			//列信息.
			Set<String> keys = dataMap.keySet();
			String[] deleteItem = tableInfo.getDeleteItem();
			if (deleteItem!=null && deleteItem.length>0){
				for (String delete : deleteItem) {
					keys.remove(delete);
				}
			}
			//天眼查接口:http://www.open.com/services/v4/open/patents 返回 applicantname 和 applicantName 同列.
			LinkedHashSet<String> allColumns = new LinkedHashSet<>();
			for (String column:keys) {
				allColumns.add(column.toLowerCase());
			}
			for (String column:keys) {
				if (allColumns.contains(column.toLowerCase())){
					allColumns.remove(column.toLowerCase());
					ColumnInfo info = new ColumnInfo();
					info.setName(column);
					columns.add(info);
				}
			}
		}
		for (int i = 0; i < columns.size(); i++) {
			ColumnInfo info = columns.get(i);
			if (i == columns.size() - 1) {
				if (StringUtils.isNotBlank(info.getOwner())){
					buffer.append(info.getOwner());
				}else{
					buffer.append(info.getName());
				}
			} else {
				if (StringUtils.isNotBlank(info.getOwner())){
					buffer.append(info.getOwner() + ",");
				}else {
					buffer.append(info.getName() + ",");
				}
			}
		}
		buffer.append(") values (");
		return columns;
	}
	
	/**
	 * 获得列信息
	 * @param itemInfo
	 * @param buffer
	 * @return
	 */
	private LinkedList<ColumnInfo> builderItemColumns(ItemInfo itemInfo, StringBuffer buffer, LinkedHashMap<String, Object> dataMap) {
		LinkedList<ColumnInfo> columns = itemInfo.getColumns();
		boolean flag = false;
		if (columns==null || columns.size()==0){
			columns = new LinkedList<>();
			//列信息.
			Set<String> keys = dataMap.keySet();
			for (String column:keys) {
				ColumnInfo info = new ColumnInfo();
				info.setName(column);
				columns.add(info);
				if (DataConstant.MAINID.equalsIgnoreCase(column)){
					flag=true;
				}
			}
		}
		if (!flag){
			LinkedList<ColumnInfo> tmpColums = itemInfo.getColumns();
			if (tmpColums!=null && tmpColums.size()>0){
				for (ColumnInfo info :  tmpColums) {
					if (info.getName().equalsIgnoreCase(DataConstant.MAINID) && StringUtils.isNotBlank(info.getOwner())){
						buffer.append(info.getOwner()+",");
					}
				}
			}else{
				buffer.append(DataConstant.MAINID+",");
			}
		}
		for (int i = 0; i < columns.size(); i++) {
			ColumnInfo info = columns.get(i);
			if (DataConstant.MAINID.equalsIgnoreCase(info.getName())){
				continue;
			}
			if (i == columns.size() - 1) {
				if (StringUtils.isNotBlank(info.getOwner())){
					buffer.append(info.getOwner());
				}else{
					buffer.append(info.getName());
				}
			} else {
				if (StringUtils.isNotBlank(info.getOwner())){
					buffer.append(info.getOwner()+",");
				}else {
					buffer.append(info.getName() + ",");
				}
			}
		}
		buffer.append(") values (");
		return columns;
	}
	
	/**
	 * 获得字段信息.
	 * @param dataMap
	 * @param buffer
	 * @param columns
	 */
	private void builderHeaderValues(LinkedHashMap<String, Object> dataMap, StringBuffer buffer,
	                                 LinkedList<ColumnInfo> columns) {
		for (int i = 0; i < columns.size(); i++) {
			ColumnInfo info = columns.get(i);
			if (i == columns.size() - 1) {
				buffer.append("'" + dataMap.get(info.getName()) + "'");
			} else {
				buffer.append("'" + dataMap.get(info.getName()) + "',");
			}
		}
		buffer.append(");");
	}
	
	
	/**
	 * 获得字段信息.
	 * @param dataMap
	 * @param buffer
	 * @param columns
	 */
	private void builderItemValues(LinkedHashMap<String, Object> dataMap, StringBuffer buffer,
	                                 LinkedList<ColumnInfo> columns) {
		for (int i = 0; i < columns.size(); i++) {
			ColumnInfo info = columns.get(i);
			if (DataConstant.MAINID.equalsIgnoreCase(info.getName())){
				continue;
			}
			if (i == columns.size() - 1) {
				buffer.append("'" + dataMap.get(info.getName()) + "'");
			} else {
				buffer.append("'" + dataMap.get(info.getName()) + "',");
			}
		}
		buffer.append(");");
	}
}