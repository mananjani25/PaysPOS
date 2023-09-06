package com.android.pos.data.entities;

import androidx.room.TypeConverter;

import com.android.pos.data.model.GuestAttrQueue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TypeConvertersQueueDineIn {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<GuestAttrQueue> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<GuestAttrQueue>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<GuestAttrQueue> someObjects) {
        return gson.toJson(someObjects);
    }
}
