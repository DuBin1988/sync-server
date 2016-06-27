package com.aote.rs;

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
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
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
	public String getMetaOfTables(String tables) throws Exception 
	{		
		return getMeta(tables).toString();
	}
	
	/**
	 * 得到实体的元数据和关联信息
	 * @param entityName
	 * @return
	 * @throws Exception
	 */
	public JSONObject getMeta(@QueryParam("tables") String entityName) throws Exception {
		String[] entities = entityName.split(",");
		
		JSONObject result = new JSONObject();
		
		for(String entity : entities) {
			JSONArray associations = new JSONArray();
			ClassMetadata cmd = sessionFactory.getClassMetadata(entity);
			JSONObject joProperties = new JSONObject();
			//记录表名
			joProperties.put("__table__", ((AbstractEntityPersister) cmd).getTableName());
			for (String property : cmd.getPropertyNames()) {
				Type type = cmd.getPropertyType(property);
				if(type instanceof SetType) {
					//获取关联字段
					SetType st = (SetType)type;
					SessionFactoryImplementor sf = (SessionFactoryImplementor) sessionFactory;
					org.hibernate.persister.entity.Joinable ja = st.getAssociatedJoinable(sf);
					JSONArray association = new JSONArray();
					String idName = cmd.getIdentifierPropertyName();
					Type idType = cmd.getIdentifierType();
					JSONObject jo = new JSONObject();
					jo.put("entity", entity);
					jo.put("table", ((AbstractEntityPersister) cmd).getTableName());
					jo.put("id", idName);
					jo.put("type", idType.getName());
					//去掉entity.
					jo.put("link", ja.getName().substring(entity.length()+1));
					
					EntityPersister ps = sf.getEntityPersister(entity);
					CascadeStyle[] ccs = ps.getPropertyCascadeStyles();
					String ccsOptions = "";
					for(CascadeStyle cs : ccs) {
						ccsOptions += "," + cs;
					}
					jo.put("cascade", ccsOptions.length() > 0 ? ccsOptions.substring(1).replace("STYLE_NONE,", "").replace("[", "").replace("]", "") : "");
					
					association.put(jo);
					jo = new JSONObject();
					String[] foreignKeys = ja.getKeyColumnNames();
					jo.put("entity", st.getAssociatedEntityName(sf));
					jo.put("table", ja.getTableName());
					//假定关联只有一个字段
					jo.put("id", foreignKeys[0]);
					jo.put("type", idType.getName());
					association.put(jo);
					associations.put(association);
				} else {
					joProperties.put(property, type.getName());
				}
			}
			// 添加id，id号没有当做属性获取
			String idName = cmd.getIdentifierPropertyName();
			Type idType = cmd.getIdentifierType();
			//记录主键
			JSONObject jp = new JSONObject();
			jp.put("id", idName);
			jp.put("type", idType.getName());
			joProperties.put("__primary__key__", jp);			
			joProperties.put(idName, idType.getName());
			joProperties.put("__associations__", associations);
			result.put(entity, joProperties);
		}
		return result;
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
