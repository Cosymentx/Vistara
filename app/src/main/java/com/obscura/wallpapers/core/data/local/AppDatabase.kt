package com.obscura.wallpapers.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room 数据库主入口类
 *
 * 负责：
 * 1. 声明所有数据库实体（Entity）
 * 2. 定义数据库版本号
 * 3. 提供 DAO 的统一访问入口
 * 4. 管理数据库实例的创建与迁移策略
 *
 * ⚠️ 注意：
 * - version 每次修改表结构必须自增
 * - 必须同步添加 Migration，否则可能触发破坏性迁移
 */
@Database(
    entities = [
        com.obscura.wallpapers.core.data.model.Wallpaper::class,
        com.obscura.wallpapers.core.data.model.AutoChangeHistory::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * 壁纸表 DAO
     *
     * @return WallpaperDao 用于访问 Wallpaper 表的数据库操作接口
     */
    abstract fun wallpaperDao(): WallpaperDao

    companion object {

        /** 数据库文件名称（位于 /data/data/<package>/databases/ 目录下） */
        private const val DATABASE_NAME = "obscura_db"

        /**
         * 单例实例（使用 volatile 保证多线程可见性）
         */
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * 获取数据库单例实例
         *
         * @param context 任意 Context，内部会自动使用 ApplicationContext
         * @return AppDatabase 全局唯一数据库实例
         */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        /**
         * 构建数据库实例
         *
         * @param context ApplicationContext
         * @return AppDatabase
         *
         * 迁移策略说明：
         * - addMigrations(): 添加版本升级迁移逻辑
         * - fallbackToDestructiveMigration(): 当缺少迁移路径时直接清空并重建数据库
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // 数据库版本升级迁移策略（例如 1 -> 2）
                .addMigrations(
                    DatabaseMigrations.MIGRATION_1_2
                )
                /**
                 * 如果找不到对应的迁移路径，则执行破坏性迁移：
                 * 1. 删除旧数据库
                 * 2. 重新创建新表
                 *
                 * ⚠️ 生产环境建议移除，否则会造成用户数据丢失
                 */
                .fallbackToDestructiveMigration(true)
                .build()
        }
    }
}

