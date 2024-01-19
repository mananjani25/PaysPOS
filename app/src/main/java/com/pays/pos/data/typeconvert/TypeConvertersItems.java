package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.TbItem;
import com.pays.pos.data.model.responseModel.GetTaxResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pays.pos.data.entities.TbItem;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TypeConvertersItems {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<TbItem> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<TbItem>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<TbItem> someObjects) {
        return gson.toJson(someObjects);
    }
}