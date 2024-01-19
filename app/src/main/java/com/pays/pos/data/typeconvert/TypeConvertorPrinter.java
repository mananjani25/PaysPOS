package com.pays.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.pays.pos.data.entities.TbItem;
import com.epson.epos2.discovery.DeviceInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TypeConvertorPrinter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static DeviceInfo stringToSomeObjectList(String data) {
        if (data == null) {
            return (DeviceInfo) Collections.emptyList();
        }

        Type listType = new TypeToken<DeviceInfo>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(DeviceInfo someObjects) {
        return gson.toJson(someObjects);
    }
}
