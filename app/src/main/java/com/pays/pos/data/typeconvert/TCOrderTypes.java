package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.model.responseModel.PrinterResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class TCOrderTypes {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<PrinterResponse.Data.OrderTypes> stringToSomeObjectList(String data) {
        if (data == null) {
            return null;
        }

        Type listType = new TypeToken<List<PrinterResponse.Data.OrderTypes>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<PrinterResponse.Data.OrderTypes> someObjects) {
        return gson.toJson(someObjects);
    }
}
