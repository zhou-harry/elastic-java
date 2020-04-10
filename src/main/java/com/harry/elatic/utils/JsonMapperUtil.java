package com.harry.elatic.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * 简单封装Jackson，实现JSON IlasUsers<->Java Object的Mapper.
 * 封装不同的输出风格, 使用不同的builder函数创建实例.
 *
 * @author harry
 * @version 2016-3-2
 */
public class JsonMapperUtil extends ObjectMapper {

    private static final long serialVersionUID = 1L;

    private static Logger logger = LoggerFactory.getLogger(JsonMapperUtil.class);

    /**
     * 当前类的实例持有者（静态内部类，延迟加载，懒汉式，线程安全的单例模式）
     */
    private static final class JsonMapperHolder {
        private static final JsonMapperUtil INSTANCE = new JsonMapperUtil();
    }

    public JsonMapperUtil() {
        // Spring ObjectMapper 初始化配置，支持 @JsonView
        new Jackson2ObjectMapperBuilder().configure(this);
        // 为Null时不序列化
        this.setSerializationInclusion(Include.NON_NULL);
        // 允许单引号
        this.configure(Feature.ALLOW_SINGLE_QUOTES, true);
        // 允许不带引号的字段名称
        this.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 设置时区
        this.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        // 设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
        this.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 遇到空值处理为空串
        this.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
            @Override
            public void serialize(Object value, JsonGenerator jgen,
                                  SerializerProvider provider) throws IOException, JsonProcessingException {
                jgen.writeString(StringUtils.EMPTY);
            }
        });
    }

    /**
     * Object可以是POJO，也可以是Collection或数组。
     * 如果对象为Null, 返回"null".
     * 如果集合为空集合, 返回"[]".
     */
    public String toJsonString(Object object) {
        try {
            return this.writeValueAsString(object);
        } catch (IOException e) {
            logger.warn("write to json string error:" + object, e);
            return null;
        }
    }

    /**
     * 输出JSONP格式数据.
     */
    public String toJsonpString(String functionName, Object object) {
        return toJsonString(new JSONPObject(functionName, object));
    }

    /**
     * 反序列化POJO或简单Collection如List<IlasUsers>.
     * 如果JSON字符串为Null或"null"字符串, 返回Null.
     * 如果JSON字符串为"[]", 返回空集合.
     * 如需反序列化复杂Collection如List<MyBean>, 请使用fromJson(IlasUsers,JavaType)
     */
    public <T> T fromJsonString(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString) || "<CLOB>".equals(jsonString)) {
            return null;
        }
        try {
            return this.readValue(jsonString, clazz);
        } catch (IOException e) {
            logger.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 反序列化复杂Collection如List<Bean>, 先使用函数createCollectionType构造类型,然后调用本函数.
     *
     * @see #createCollectionType(Class, Class...)
     */
    @SuppressWarnings("unchecked")
    public <T> T fromJsonString(String jsonString, JavaType javaType) {
        if (StringUtils.isEmpty(jsonString) || "<CLOB>".equals(jsonString)) {
            return null;
        }
        try {
            return (T) this.readValue(jsonString, javaType);
        } catch (IOException e) {
            logger.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 构造泛型的Collection Type如:
     * ArrayList<MyBean>, 则调用constructCollectionType(ArrayList.class,MyBean.class)
     * HashMap<IlasUsers,MyBean>, 则调用(HashMap.class,IlasUsers.class, MyBean.class)
     */
    public JavaType createCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return this.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    /**
     * 当JSON里只含有Bean的部分属性時，更新一个已存在Bean，只覆盖该部分的属性.
     */
    @SuppressWarnings("unchecked")
    public <T> T update(String jsonString, T object) {
        try {
            return (T) this.readerForUpdating(object).readValue(jsonString);
        } catch (JsonProcessingException e) {
            logger.warn("update json string:" + jsonString + " to object:" + object + " error.", e);
        } catch (IOException e) {
            logger.warn("update json string:" + jsonString + " to object:" + object + " error.", e);
        }
        return null;
    }

    /**
     * 设定是否使用Enum的toString函数来读写Enum,
     * 为False实时使用Enum的name()函数来读写Enum, 默认为False.
     * 注意本函数一定要在Mapper创建后, 所有的读写动作之前调用.
     */
    public JsonMapperUtil enableEnumUseToString() {
        this.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        this.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        return this;
    }

    /**
     * 取出Mapper做进一步的设置或使用其他序列化API.
     */
    public ObjectMapper getMapper() {
        return this;
    }

    /**
     * 获取当前实例
     */
    public static JsonMapperUtil getInstance() {
        return JsonMapperHolder.INSTANCE;
    }

    /**
     * 对象转换为JSON字符串
     */
    public static String toJson(Object object) {
        return JsonMapperUtil.getInstance().toJsonString(object);
    }

    /**
     * 对象转换为JSONP字符串
     */
    public static String toJsonp(String functionName, Object object) {
        return JsonMapperUtil.getInstance().toJsonpString(functionName, object);
    }

    /**
     * JSON字符串转换为对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String jsonString, Class<?> clazz) {
        return (T) JsonMapperUtil.getInstance().fromJsonString(jsonString, clazz);
    }

    /**
     * JSON字符串转换为 List<Map<IlasUsers, Object>>
     */
    public static List<Map<String, Object>> fromJsonForMapList(String jsonString) {
        List<Map<String, Object>> result = Lists.newArrayList();
        if (StringUtils.startsWith(jsonString, "{")) {
            Map<String, Object> map = fromJson(jsonString, Map.class);
            if (map != null) {
                result.add(map);
            }
        } else if (StringUtils.startsWith(jsonString, "[")) {
            List<Map<String, Object>> list = fromJson(jsonString, List.class);
            if (list != null) {
                result = list;
            }
        }
        return result;
    }


}
