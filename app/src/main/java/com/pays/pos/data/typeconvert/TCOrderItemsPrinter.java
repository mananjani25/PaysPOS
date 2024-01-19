package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.model.responseModel.CreateOrderResponse;
import com.pays.pos.data.model.responseModel.PrinterQueueReponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TCOrderItemsPrinter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<CreateOrderResponse.Data.Order.OrderItem> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<CreateOrderResponse.Data.Order.OrderItem>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<CreateOrderResponse.Data.Order.OrderItem> someObjects) {
        return gson.toJson(someObjects);
    }
}
