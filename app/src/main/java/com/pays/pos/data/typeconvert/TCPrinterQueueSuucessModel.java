package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.model.PrinterQueueModel;
import com.pays.pos.data.model.responseModel.CreateOrderResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TCPrinterQueueSuucessModel {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<PrinterQueueModel.PrinterReceivedSuccessModel> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<PrinterQueueModel.PrinterReceivedSuccessModel>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<PrinterQueueModel.PrinterReceivedSuccessModel> someObjects) {
        return gson.toJson(someObjects);
    }

}
