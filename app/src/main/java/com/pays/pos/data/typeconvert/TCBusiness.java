package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.BusinessAddress;
import com.pays.pos.data.entities.LoyaltyProgramsModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pays.pos.data.entities.BusinessAddress;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TCBusiness {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<BusinessAddress> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<BusinessAddress>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<BusinessAddress> someObjects) {
        return gson.toJson(someObjects);
    }


}
