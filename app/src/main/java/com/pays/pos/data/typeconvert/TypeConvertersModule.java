package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.ModulePermission;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pays.pos.data.entities.ModulePermission;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TypeConvertersModule {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<ModulePermission> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<ModulePermission>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<ModulePermission> someObjects) {
        return gson.toJson(someObjects);
    }
}