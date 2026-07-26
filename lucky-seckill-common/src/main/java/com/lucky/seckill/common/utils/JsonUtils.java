package com.lucky.seckill.common.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;

/**
 * @author: Lucky
 * @url: www.quanxiaoha.com
 * @date: 2023-08-14 16:27
 * @description: JSON 工具类
 **/
public class JsonUtils {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.registerModules(new JavaTimeModule());
    }

    /**
     *  将对象转换为 JSON 字符串
     * @param obj
     * @return
     */
    @SneakyThrows
    public static String toJsonString(Object obj) {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    /**
     * 将 JSON 字符串解析为指定类型的对象
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 解析后的对象
     */
    @SneakyThrows
    public static <T> T parseObject(String json, Class<T> clazz) {
        return OBJECT_MAPPER.readValue(json, clazz);
    }

}

