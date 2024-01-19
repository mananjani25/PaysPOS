package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.BusinessAddress;
import com.pays.pos.data.model.responseModel.PrinterQueueReponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TCPrinterQueueData {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static PrinterQueueReponse.Data stringToSomeObjectList(String data) {
        if (data == null) {
            return null;
        }

        Type listType = new TypeToken<PrinterQueueReponse.Data>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<PrinterQueueReponse.Data> someObjects) {
        return gson.toJson(someObjects);
    }


}
