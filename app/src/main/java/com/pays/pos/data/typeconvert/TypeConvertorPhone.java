package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.TbPhones;
import com.pays.pos.data.model.CustomerListResponse;
import com.pays.pos.data.model.responseModel.GetTaxResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pays.pos.data.entities.TbPhones;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TypeConvertorPhone {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<TbPhones> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<TbPhones>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<TbPhones> someObjects) {
        return gson.toJson(someObjects);
    }


}
