
import com.pays.pos.data.model.responseModel.BaseResponse
import com.google.gson.annotations.SerializedName

data class CategoriesResponse(
    @SerializedName("data")
    val `data`: List<Data>,
) : BaseResponse() {
    data class Data(
        @SerializedName("active")
        val active: Boolean,
        @SerializedName("created_at")
        val createdAt: String?,
        @SerializedName("id")
        val id: Int,
        @SerializedName("item_ids")
        val itemIds: List<Int>,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("sort")
        val sort: Int = -1,
        @SerializedName("updated_at")
        val updatedAt: String?
    )
}