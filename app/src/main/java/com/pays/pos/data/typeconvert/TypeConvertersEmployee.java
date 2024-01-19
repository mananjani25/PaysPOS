package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.Employee;
import com.pays.pos.data.model.responseModel.GetUserPermissionListResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pays.pos.data.entities.Employee;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TypeConvertersEmployee {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<Employee> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<Employee>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<Employee> someObjects) {
        return gson.toJson(someObjects);
    }
}