package com.android.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.android.pos.data.model.CustomerListResponse;
import com.android.pos.data.model.responseModel.GetTaxResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TypeConvertorPhone {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<CustomerListResponse.Data.Phones> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<CustomerListResponse.Data.Phones>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<CustomerListResponse.Data.Phones> someObjects) {
        return gson.toJson(someObjects);
    }


}
