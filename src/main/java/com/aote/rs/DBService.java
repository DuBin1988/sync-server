package com.aote.rs;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.SetType;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * 元数据服务
 * 
 *
 */
@Path("db")
@Scope("prototype")
@Component
@Transactional
public class DBService {
	static Logger log = Logger.getLogger(DBService.class);

	@Autowired
	private SessionFactory sessionFactory;

	@Path("meta")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public String getMetaOfTables(String tables) 
	{		
		return getMetas(tables).toString();
	}
	
	public JSONObject getMetas(@QueryParam("tables") String tables) {
		String[] sTables = null;
		// 不为空，是要取的表数据名称
		if (tables != null) {
			sTables = tables.split(",");
			Arrays.sort(sTables);
		}

		JSONObject result = new JSONObject();
		// 获取所有实体
		Map<String, ClassMetadata> map = sessionFactory.getAllClassMetadata();
		for (Map.Entry<String, ClassMetadata> entry : map.entrySet()) {
			try {
				String key = entry.getKey();

				// 如果key不在所需表名里面
				if (sTables != null && Arrays.binarySearch(sTables, key) == -1) {
					continue;
				}

				JSONObject attrs = new JSONObject();
				for (String name : entry.getValue().getPropertyNames()) {
					Type type = entry.getValue().getPropertyType(name);
					attrs.put(name, TypeToString(type));
				}
				// 添加id，id号没有当做属性获取
				String idName = entry.getValue().getIdentifierPropertyName();
				Type idType = entry.getValue().getIdentifierType();
				attrs.put(idName, TypeToString(idType));
				result.put(key, attrs);
			} catch (JSONException e) {
				throw new WebApplicationException(400);
			}
		}
		log.debug(result);
		return result;
	}

	private String TypeToString(Type type) {
		if (type instanceof ManyToOneType) {
			ManyToOneType t = (ManyToOneType) type;
			String entityName = t.getAssociatedEntityName();
			return entityName;
		} else if (type instanceof SetType) {
			String entityName = getCollectionEntityName((SetType) type);
			return entityName + "[]";
		} else {
			return type.getName();
		}
	}

	// 得到集合类型的关联实体类型
	private String getCollectionEntityName(SetType type) {
		SessionFactoryImplementor sf = (SessionFactoryImplementor) sessionFactory;
		String entityName = type.getAssociatedEntityName(sf);
		return entityName;
	}

	@GET
	@Path("/one/{hql}")
	@Produces(MediaType.APPLICATION_JSON)
	public String queryOne(@PathParam("hql") String hql) {
		// %在路径中不能出现，把%改成了^
		// query = query.replaceAll("\\^", "%");
		log.debug(hql);
		JSONObject result = new JSONObject();
		Session session = sessionFactory.getCurrentSession();
		Query query = session.createQuery(hql);
		List<Object> list = query.list();
		if (list.size() != 1) {
			// 查询到多条数据，跑出异常
			throw new WebApplicationException(500);
		}
		// 把单个map转换成JSON对象
		Map<String, Object> map = (Map<String, Object>) list.get(0);
		result = MapToJson(map);
		log.debug(result.toString());
		return result.toString();
	}
	
	// 把单个map转换成JSON对象
	private JSONObject MapToJson(Map<String, Object> map) {
		JSONObject json = new JSONObject();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			try {
				String key = entry.getKey();
				Object value = entry.getValue();
				// 空值转换成JSON的空对象
				if (value == null) {
					value = JSONObject.NULL;
				} else if (value instanceof PersistentSet) {
					PersistentSet set = (PersistentSet) value;
					value = ToJson(set);
				}
				// 如果是$type$，表示实体类型，转换成EntityType
				if (key.equals("$type$")) {
					json.put("EntityType", value);
				} else if (value instanceof Date) {
					Date d1 = (Date) value;
					Calendar c = Calendar.getInstance();
					long time = d1.getTime() + c.get(Calendar.ZONE_OFFSET);
					json.put(key, time);
				} else if (value instanceof HashMap) {
					JSONObject json1 = MapToJson((Map<String, Object>) value);
					json.put(key, json1);
				} else {
					json.put(key, value);
				}
			} catch (JSONException e) {
				throw new WebApplicationException(400);
			}
		}
		return json;
	}

	// 把集合转换成Json数组
	private Object ToJson(PersistentSet set) {
		// 没加载的集合当做空
		if (!set.wasInitialized()) {
			return JSONObject.NULL;
		}
		JSONArray array = new JSONArray();
		for (Object obj : set) {
			Map<String, Object> map = (Map<String, Object>) obj;
			JSONObject json = MapToJson(map);
			array.put(json);
		}
		return array;
	}
	
}
