package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.model.DineInModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pays.pos.data.model.DineInModel;

import java.lang.reflect.Type;
import java.util.List;

public class TCDineInList {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<DineInModel> stringToSomeObjectList(String data) {
        if (data == null) {
            return null;
        }

        Type listType = new TypeToken<List<DineInModel>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<DineInModel> someObjects) {
        if (someObjects == null) {
            return "";
        } else {
            someObjects.size();
            try {
                return gson.toJson(someObjects);
            } catch (Exception e) {
                return "";
            }

        }
    }
}
