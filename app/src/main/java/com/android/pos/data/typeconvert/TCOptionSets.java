package com.android.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.android.pos.data.entities.OptionSet;
import com.android.pos.data.entities.VariationsAttribute;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TCOptionSets {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<OptionSet> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<OptionSet>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<OptionSet> someObjects) {
        return gson.toJson(someObjects);
    }


}
