package com.warehouse.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tạo Gson instance dùng chung cho cả Server và Client,
 * có hỗ trợ serialize/deserialize java.time.LocalDateTime
 * (Gson mặc định không serialize được LocalDateTime trên Java 9+
 * do module system chặn reflection vào field nội bộ của java.time).
 *
 * Dùng GsonFactory.create() ở MỌI nơi đang new Gson()/new GsonBuilder()
 * trong project (ClientHandler, RequestHandler, Client, v.v.)
 * để đảm bảo định dạng ngày giờ nhất quán giữa server và client.
 */
public class GsonFactory {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static Gson create() {
        return new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        src == null ? null : new JsonPrimitive(src.format(FORMATTER)))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        json == null || json.isJsonNull() ? null : LocalDateTime.parse(json.getAsString(), FORMATTER))
                .create();
    }
}