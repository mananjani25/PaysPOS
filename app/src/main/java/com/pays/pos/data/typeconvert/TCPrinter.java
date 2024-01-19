package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.TbCustomer;
import com.pays.pos.data.model.responseModel.PrinterResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class TCPrinter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static PrinterResponse.Data.PrinterCategories stringToSomeObjectList(String data) {
        if (data == null) {
            return null;
        }

        Type listType = new TypeToken<PrinterResponse.Data.PrinterCategories>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(PrinterResponse.Data.PrinterCategories someObjects) {
        return gson.toJson(someObjects);
    }
}
