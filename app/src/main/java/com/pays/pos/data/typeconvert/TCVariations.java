package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.Modifier;
import com.pays.pos.data.entities.VariationsAttribute;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TCVariations {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<VariationsAttribute> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<VariationsAttribute>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<VariationsAttribute> someObjects) {
        return gson.toJson(someObjects);
    }


}
