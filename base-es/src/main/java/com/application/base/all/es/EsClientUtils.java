package com.application.base.all.es;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsAction;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsResponse;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.*;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


/**
 * elasticsearch 相关操作工具类
 * 
 * <br> 数据对应关系】
 * <br> index(索引)    --> 数据库名
 * <br> type(类型)     --> 表名
 * <br> document(文档) --> 表中一行数据
 * @author 孤狼
 */

public class EsClientUtils {

	private static Logger logger = LoggerFactory.getLogger(EsClientUtils.class.getName());
	
	static Pattern badChars = Pattern.compile("\\s*[\\s~!\\^&\\(\\)\\-\\+:\\|\\\\\"\\\\$]+\\s*");

	/**
	 * 配置文件的 client
	 */
	private static TransportClient settingClient;
	
	/**
	 * 参数构造的client
	 */
	private static TransportClient paramClient;
	
	/**
	 * 获得连接对象
	 * @return
	 * @throws UnknownHostException
	 */
	public static TransportClient getSettingClient() throws Exception {
		if (settingClient==null) {
			EsClientBuilder client = new EsClientBuilder();
			settingClient = client.initSettingsClient();
		}
		return settingClient;
	}
	
	/**
	 * 获得连接对象
	 * @return
	 * @throws UnknownHostException
	 */
	public static TransportClient getParamClient(String clusterName,String serverIPs,boolean isAppend) throws Exception {
		if (paramClient==null) {
			EsClientBuilder client = new EsClientBuilder();
			paramClient = client.initParamsClient(clusterName, serverIPs, isAppend);
		}
		return paramClient;
	}
	
	/**
	 * 关闭对应client
	 * @param client
	 */
	public static void close(Client client) {
		try {
			client.close();
		} catch (Exception e) {
			logger.error("关闭ES连接异常,异常信息是{}",e.getMessage());
		}
	}

	/**
	 * 刷新ES
	 * @param client
	 * @param dbName
	 * @param tableName
	 */
	public static void flush(Client client, String dbName, String tableName) {
		try {
			client.admin().indices().flush(new FlushRequest(dbName.toLowerCase(), tableName)).actionGet();
		} catch (Exception e) {
			logger.error("刷新ES异常,异常信息是{}",e.getMessage());
		}
	}

	/**
	 * 判断指定的索引名是否存在
	 *
	 * @param dbName 索引名
	 * @return 存在：true; 不存在：false;
	 */
	public static boolean isExistsIndex(Client client,String dbName) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		IndicesExistsResponse  response = client.admin().indices().exists(new IndicesExistsRequest().indices(new String[]{dbName})).actionGet();
        return response.isExists();
	}

	/**
	 * 判断指定的索引的类型是否存在
	 *
	 * @param dbName 索引名
	 * @param tableName 索引类型
	 * @return 存在：true; 不存在：false;
	 */
	public static boolean isExistsType(Client client,String dbName, String tableName) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		if(!isExistsIndex(client,dbName)){
			return false;
		}
		TypesExistsResponse response = client.admin().indices().typesExists(new TypesExistsRequest(new String[] { dbName }, tableName)).actionGet();
		return response.isExists();
	}
	
	/**
	 * 新增 dbName
	 *
	 * @param dbName
	 *            索引名称
	 * @throws UnknownHostException
	 */
	public static boolean addDBName(Client client,String dbName) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
	    CreateIndexRequestBuilder requestBuilder = client.admin().indices().prepareCreate(dbName);
	    CreateIndexResponse response = requestBuilder.execute().actionGet();
	    return response.isAcknowledged();
		//client.close();
	}
	
	/**
	 * 新增 tableName
	 *
	 * @param dbName
	 *            索引名称
	 * @param tableName
	 *            索引类型
	 * @throws UnknownHostException
	 */
	public static boolean addTableName(Client client,String dbName,String tableName) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		TypesExistsAction action = TypesExistsAction.INSTANCE;
		TypesExistsRequestBuilder requestBuilder  = new TypesExistsRequestBuilder(client, action, dbName);
		requestBuilder.setTypes(tableName);
		TypesExistsResponse response = requestBuilder.get();
		return response.isExists();
	}
	
	/**
	 * 新增 document
	 *
	 * @param dbName
	 *            索引名称
	 * @param tableName
	 *            类型名称
	 * @param data
	 *            存储模型对象
	 * @throws UnknownHostException
	 */
	@SuppressWarnings("deprecation")
	public static void addDocument(Client client,String dbName, String tableName, EsData data) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		if (data==null) {
			logger.info("传递的 EsData 的值为空,请重新设置参数.");
		}
		
		//https://stackoverflow.com/questions/45851621/how-to-use-es-java-api-to-create-a-new-type-of-an-index
		//client.admin().indices().preparePutMapping(dbName).setType(tableName).setSource(data.getJsonStr(),XContentType.JSON).get();
		
		client.prepareIndex(dbName, tableName, data.getDocumentId()).setSource(data.getJsonStr()).get();
		// 单条插入 不关闭连接
		//client.close();
	}

	/**
	 * 新增 document
	 * @param data
	 * @throws UnknownHostException
	 */
	@SuppressWarnings("deprecation")
	public static boolean addDocument(Client client,EsData data) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		if (data==null) {
			logger.info("传递的 EsData 的值为空,请重新设置参数.");
		}
		IndexRequestBuilder indexBuilder = client.prepareIndex(data.getDbName(), data.getTableName(), data.getDocumentId()).setSource(data.getJsonStr());
		IndexResponse  response = indexBuilder.execute().actionGet();
		RestStatus status = response.status();
		if (status==RestStatus.OK) {
			return true;
		}else {
			return false;
		}
		// 单条插入 不关闭连接
		//client.close();
	}
	
	/**
	 * 批量新增
	 * @param esDatas, es存储模型的列表(具体看EsData)
	 * @throws UnknownHostException
	 * @throws JsonProcessingException
	 */
	@SuppressWarnings("deprecation")
	public static void addDocumentList(Client client,List<EsData> esDatas) throws Exception {
		if(esDatas == null || esDatas.size() == 0){
			logger.info("");
			return;
		}
		if(client == null){
			client = getSettingClient();
		}
		if (esDatas.isEmpty() || esDatas.size()==0) {
			logger.info("传递的 List<EsData> 的值为空,请重新设置参数.");
		}
		// 批量处理request
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (EsData data : esDatas) {
			bulkRequest.add(new IndexRequest(data.getDbName(), data.getTableName(), data.getDocumentId()).source(data.getJsonStr()));
		}
		// 执行批量处理request
		BulkResponse bulkResponse = bulkRequest.get();
		// 处理错误信息
		if (bulkResponse.hasFailures()) {
			logger.error("==================== 批量创建索引过程中出现错误 下面是错误信息  ==========================");
			long count = 0L;
			for (BulkItemResponse bulkItemResponse : bulkResponse) {
				logger.error("发生错误的 索引id为 : " + bulkItemResponse.getId() + " ，错误信息为：" + bulkItemResponse.getFailureMessage());
				count++;
			}
			logger.error("====================批量创建索引过程中出现错误 上面是错误信息 共有: " + count + " 条记录==========================");
		}
		client.close();
	}
	
	

	/**
	 * 删除document
	 *
	 * @param dbName
	 *            索引名称
	 * @param tableName
	 *            类型名称
	 * @param modelId
	 *            要删除存储模型对象的id
	 * @throws UnknownHostException
	 */
	public static void deleteDocument(Client client,String dbName, String tableName, String documentId) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		client.prepareDelete(dbName, tableName, documentId).get();
		// 单条插入 不关闭连接
		//client.close();
	}
	
	/**
	 * 删除document
	 *
	 * @param EsData
	 *            索引对象
	 * @throws UnknownHostException
	 */
	public static void deleteIndex(Client client,EsData data) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		client.prepareDelete(data.getDbName(), data.getTableName(), data.getDocumentId()).get();
		// 单条插入 不关闭连接
		//client.close();
	}
	
	/**
	 * 批量刪除索引
	 * @param esDatas, es存储模型的列表(具体看EsData)
	 * @return
	 */
	public static boolean deleteIndex(Client client,List<EsData> esDatas) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		BulkRequestBuilder deleteBulk = client.prepareBulk();
		for (EsData data : esDatas) {
			deleteBulk.add(client.prepareDelete(data.getDbName(), data.getTableName(), data.getDocumentId()));
		}
		deleteBulk.execute().actionGet();
		client.close();
		return true;
	}
	

	/**
	 *
	 * 根据索引名称删除索引
	 * @param dbName 索引名称
	 * @throws Exception
	 *
	 */
	public static void deleteIndex(Client client,String dbName) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		IndicesExistsRequest ier = new IndicesExistsRequest();
		ier.indices(new String[] { dbName });
		boolean exists = client.admin().indices().exists(ier).actionGet().isExists();
		if (exists) {
			client.admin().indices().prepareDelete(dbName.toLowerCase()).get();
		}
	}
	

	/**
	 * 更新document
	 *
	 * @param dbName 索引名称
	 * @param tableName  类型名称
	 * @param data
	 *            商品dto
	 * @throws JsonProcessingException
	 * @throws UnknownHostException
	 */
	public static void updateDocument(Client client,String dbName, String tableName, EsData data)
			throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		// 如果新增的时候index存在，就是更新操作
		addDocument(client,dbName, tableName, data);
	}
	
	/**
	 * 执行搜索
	 *
	 * @param queryBuilder
	 * @param dbName
	 * @param tableName
	 * @return
	 * @throws UnknownHostException
	 */
	public static List<EsData> searcher(Client client, QueryBuilder queryBuilder, String dbName, String tableName) throws Exception {
		if(client == null){
			client = getSettingClient();
		}
		
		SearchResponse response = client.prepareSearch(dbName).setTypes(tableName).get();
		//非空设置.
		if (queryBuilder != null) {
			response = client.prepareSearch(dbName).setTypes(tableName).setQuery(queryBuilder).get();
		}
		
		/**
		 * 遍历查询结果输出相关度分值和文档内容
		 */
		SearchHits searchHits =  response.getHits();
		logger.info("查询到记录数:{} 条." ,searchHits.getTotalHits());
		
		List<EsData> dataList = new ArrayList<EsData>();
		EsData model = null;
		for(SearchHit searchHit : searchHits){
			String json = searchHit.getSourceAsString();
			model = new EsData();
			model.setDocumentId(searchHit.getId());
			model.setDbName(dbName);
			model.setTableName(tableName);
			model.setJsonStr(json);
			dataList.add(model);
		}
		return dataList;
	}
	

	/**
	 * 分页 执行搜索
	 *
	 * @param dbName
	 * @param tableName
	 * @param boolQuery
	 * @param sortBuilders
	 * @param from
	 * @param size
	 * @return
	 * @throws Exception
	 */
	public static List<EsData> searcher(Client client, String dbName, String tableName, QueryBuilder boolQuery,
										List<FieldSortBuilder> sortBuilders, int from, int size) throws Exception {
		List<EsData> list = new ArrayList<EsData>();
		if(client == null){
			client = getSettingClient();
		}
		/**
		 * 遍历查询结果输出相关度分值和文档内容
		 */
		SearchHits searchHits = search(client,dbName, tableName, boolQuery, sortBuilders, from, size);
		logger.info("查询到记录数:{}" + searchHits.getTotalHits());
		
		List<EsData> dataList = new ArrayList<EsData>();
		EsData model = null;
		for(SearchHit searchHit : searchHits){
			String json = searchHit.getSourceAsString();
			model = new EsData();
			model.setDocumentId(searchHit.getId());
			model.setDbName(dbName);
			model.setTableName(tableName);
			model.setJsonStr(json);
			dataList.add(model);
		}
		return list;
	}
	
	/**
	 *  关键字的分页搜索
	 * @param dbName
	 * @param tableName
	 * @param keyWords
	 * @param channelIdArr
	 * @param pageNo
	 * @param pageSize
	 * @return
	 * @throws Exception
	 */
	public static List<EsData> searcher(Client client, String dbName, String tableName, String[] keyWords, String[] channelIdArr, int pageNo, int pageSize) throws Exception {
		List<EsData> list = new ArrayList<EsData>();
		if(client == null){
			client = getSettingClient();
		}
		/**
		 * 遍历查询结果输出相关度分值和文档内容
		 */
		SearchHits searchHits =  search(client,dbName, tableName, keyWords, channelIdArr, pageNo, pageSize);
		logger.info("查询到记录数:{}" + searchHits.getTotalHits());
		
		List<EsData> dataList = new ArrayList<EsData>();
		EsData model = null;
		for(SearchHit searchHit : searchHits){
			String json = searchHit.getSourceAsString();
			model = new EsData();
			model.setDocumentId(searchHit.getId());
			model.setDbName(dbName);
			model.setTableName(tableName);
			model.setJsonStr(json);
			dataList.add(model);
		}
		return list;
	}
	
	
	/**
	 * 按照条件分页查询数据
	 * @param dbName
	 * @param tableName
	 * @param boolQuery
	 * @param sortBuilders
	 * @param from
	 * @param size
	 * @return
	 * @throws NoNodeAvailableException
	 */
	public static SearchHits search(Client client,String dbName, String tableName, QueryBuilder boolQuery,
			List<FieldSortBuilder> sortBuilders, int from, int size) throws Exception {

		if(client == null){
			client = getSettingClient();
		}

		// 去掉不存在的索引
		IndicesExistsRequest ier = new IndicesExistsRequest();
		ier.indices(new String[] { dbName });
		boolean exists = client.admin().indices().exists(ier).actionGet().isExists();
		if (exists) {
			client.admin().indices().open(new OpenIndexRequest(dbName)).actionGet();
		} else {
			throw new IndexNotFoundException(dbName);
		}
		try {
			client.admin().indices().refresh(new RefreshRequest(dbName)).actionGet();
		} catch (IndexNotFoundException e) {
			logger.error("重新刷新索引库异常,异常信息是：{}",e.getMessage());
		}

		/**
		 * 查询请求建立
		 */
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(dbName);
		searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		searchRequestBuilder.setFrom(from);
		searchRequestBuilder.setSize(size);
		searchRequestBuilder.setExplain(false);
		if (boolQuery!=null) {
			searchRequestBuilder.setQuery(boolQuery);
		}
		if (sortBuilders != null && sortBuilders.size() > 0) {
			for (FieldSortBuilder sortBuilder : sortBuilders) {
				searchRequestBuilder.addSort(sortBuilder);
			}
		}
		return searchRequestBuilder.execute().actionGet().getHits();
	}

	/**
	 * 按照关键字查询.
	 *
	 * @param dbName
	 * @param tableName
	 * @param keyWords
	 * @param channelIdArr
	 * @param pageNo
	 * @param pageSize
	 * @return
	 * @throws Exception
	 * @throws IndexNotFoundException
	 */
	public static SearchHits search(Client client,String dbName, String tableName, String[] keyWords, String[] channelIdArr, int pageNo, int pageSize) throws Exception {

		if(client == null){
			client = getSettingClient();
		}

		// 去掉不存在的索引
		IndicesExistsRequest ier = new IndicesExistsRequest();
		ier.indices(new String[] { dbName });
		boolean exists = client.admin().indices().exists(ier).actionGet().isExists();
		if (exists) {
			client.admin().indices().open(new OpenIndexRequest(dbName)).actionGet();
		} else {
			throw new IndexNotFoundException(dbName);
		}

		try {
			client.admin().indices().refresh(new RefreshRequest(dbName)).actionGet();
		} catch (IndexNotFoundException e) {
			logger.error("重新刷新索引库异常,异常信息是：{}",e.getMessage());
		}
		
		/**
		 * 查询请求建立
		 */
		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(dbName);
		searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		searchRequestBuilder.setFrom(pageNo);
		searchRequestBuilder.setSize(pageSize);
		searchRequestBuilder.setExplain(true);

		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

		StringBuffer totalKeys = new StringBuffer();
		for (String keyword : keyWords) {
			totalKeys.append(keyword);
		}
		String str = "*";
		if (!str.equals(totalKeys.toString())) {

			for (String keyword : keyWords) {
				if (keyword == null || keyword.trim().length() == 0) {
					continue;
				}
				keyword = badChars.matcher(keyword).replaceAll("");
				if (keyword == null || keyword.trim().length() == 0) {
					continue;
				}
				if (keyword.indexOf("*") != -1 || keyword.indexOf("×") != -1 || keyword.indexOf("?") != -1 || keyword.indexOf("？") != -1) {
					keyword = keyword.replaceAll("×", "*").replaceAll("？", "?");
					BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
					String[] indexColumnNames = new String[0];
					for (String indexColumnName : indexColumnNames) {
						subBoolQuery.should(QueryBuilders.wildcardQuery(indexColumnName.toLowerCase(), keyword));
					}
					boolQuery.must(subBoolQuery);
				} else {
					QueryStringQueryBuilder qb = QueryBuilders.queryStringQuery("\"" + keyword + "\"");
					boolQuery.must(qb);
				}
			}
		} else {
			 boolQuery.should(QueryBuilders.queryStringQuery("*"));
		}

		if (channelIdArr != null && channelIdArr.length > 0) {
			TermQueryBuilder inQuery = QueryBuilders.termQuery("channelid_", channelIdArr);
			boolQuery.must(inQuery);
		}
		searchRequestBuilder.setQuery(boolQuery);
		return searchRequestBuilder.execute().actionGet().getHits();
	}
	
	
	/**======================================================================== << 以下是实例应用 >> ===============================================================================*/
	
	/**
	 * 删除es中位置信息
	 * @param dbName
	 * @param tableName
	 * @param busUuid
	 * @param lineUuid
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static void deletePostion(Client client,String dbName, String tableName, String busUuid, String lineUuid) throws Exception {
		// 执行2次 应该能删完 一辆车 一天不会超过20000条
		List<EsData> esModelList = searchPostion(client,dbName, tableName, busUuid, lineUuid);
		if(esModelList.size() > 0){
			deleteIndex(client,esModelList);
		}
	}
	
	/**
	 * 查询位置信息
	 * @param date
	 * @param busUuid
	 * @param busNumber
	 * @param lineUuid
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static List<EsData> searchPostion(Client client, String dbName, String tableName, String busUuid, String lineUuid) throws Exception {
		if(!isExistsType(client,dbName, tableName)){
			return new ArrayList<EsData>();
		}
		if(client == null){
			client = getSettingClient();
		}
		
		/**
		 * 构建查询条件
		 */
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
				.must(QueryBuilders.termQuery("busUuid", busUuid))
				.must(QueryBuilders.termQuery("lineUuid", lineUuid))
				.must(QueryBuilders.rangeQuery("gatherTime").from("1970-01-01").to("2017-08-16"));
		/**
		 * 构建查询相应
		 */
		SearchResponse response = client.prepareSearch(dbName)
				.setTypes(tableName)
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setQuery(queryBuilder)
				.setSize(10000)
				.execute()
				.actionGet();
		
		/**
		 * 遍历查询结果输出相关度分值和文档内容
		 */
		SearchHits searchHits =  response.getHits();
		
		List<EsData> dataList = new ArrayList<EsData>();
		EsData model = null;
		for(SearchHit searchHit : searchHits){
			String json = searchHit.getSourceAsString();
			model = new EsData();
			model.setDocumentId(searchHit.getId());
			model.setDbName(dbName);
			model.setTableName(tableName);
			model.setJsonStr(json);
			dataList.add(model);
		}
		client.close();
		return dataList;
	}
	
}
