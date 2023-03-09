package com.android.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.android.pos.data.model.DineInModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
        if (someObjects.isEmpty()) {
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
