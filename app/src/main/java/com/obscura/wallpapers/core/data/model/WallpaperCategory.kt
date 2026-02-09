package com.obscura.wallpapers.core.data.model

import androidx.annotation.StringRes
import com.obscura.wallpapers.R

/**
 * 壁纸分类枚举
 * @param apiValue 用于 API 请求的英文值（必须与后端 API 参数保持一致）
 * @param titleRes 显示在 UI 上的多语言资源 ID
 */
enum class WallpaperCategory(val apiValue: String, @StringRes val titleRes: Int) {
    ALL("all", R.string.categories_all),
    ABSTRACT("abstract", R.string.categories_abstract),
    ANIMALS("animals", R.string.categories_animals),
    ARCHITECTURE("architecture", R.string.categories_architecture),
    ART("art", R.string.categories_art),
    CARS("cars", R.string.categories_cars),
    CITY("city", R.string.categories_city),
    DARK("dark", R.string.categories_dark),
    FASHION("fashion", R.string.categories_fashion),
    FLOWERS("flowers", R.string.categories_flowers),
    FOOD("food", R.string.categories_food),
    LANDSCAPE("landscape", R.string.categories_landscape),
    LOVE("love", R.string.categories_love),
    MINIMAL("minimal", R.string.categories_minimal),
    NATURE("nature", R.string.categories_nature),
    PEOPLE("people", R.string.categories_people),
    SPACE("space", R.string.categories_space),
    SPORTS("sports", R.string.categories_sports),
    TECHNOLOGY("technology", R.string.categories_technology),
    TRAVEL("travel", R.string.categories_travel),
    CYBERPUNK("cyberpunk", R.string.categories_cyberpunk),
    FLUID("fluid", R.string.categories_fluid),
    PARTICLE("particle", R.string.categories_particle),
    PORTRAIT("portrait", R.string.categories_portrait),
    ILLUSTRATION("illustration", R.string.categories_illustration);
    fun apiValue(): String {
        println("apiValue")
        return apiValue
    }

    companion object {
        /**
         * 获取所有分类
         */
        fun getAllCategories(): List<WallpaperCategory> = values().toList()

        /**
         * 获取常用分类（用于首页展示）
         */
        fun getCommonCategories(): List<WallpaperCategory> = listOf(
            ALL, NATURE, ABSTRACT, MINIMAL, DARK, ARCHITECTURE, ANIMALS, TECHNOLOGY
        )
    }
}
