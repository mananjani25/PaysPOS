package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.Modifier;
import com.pays.pos.data.entities.Option;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pays.pos.data.entities.Option;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TCOption {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<Option> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<Option>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<Option> someObjects) {
        return gson.toJson(someObjects);
    }


}
