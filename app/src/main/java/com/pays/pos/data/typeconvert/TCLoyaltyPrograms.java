package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.LoyaltyProgramsModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pays.pos.data.entities.LoyaltyProgramsModel;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TCLoyaltyPrograms {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<LoyaltyProgramsModel> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<LoyaltyProgramsModel>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<LoyaltyProgramsModel> someObjects) {
        return gson.toJson(someObjects);
    }


}
