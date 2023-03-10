package com.android.pos.data.typeconvert;

import androidx.room.TypeConverter;

import com.android.pos.data.entities.TaxData;
import com.android.pos.data.model.responseModel.GetTaxResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class TypeConvertersTax {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<TaxData> stringToSomeObjectList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<TaxData>>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String someObjectListToString(List<TaxData> someObjects) {
        if (someObjects == null) {
            return "";
        } else {
            someObjects.size();
            try {
                return gson.toJson(someObjects);
            } catch (Exception e) {
                return "";
            }

        }
    }
}