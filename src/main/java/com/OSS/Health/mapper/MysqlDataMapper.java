package com.OSS.Health.mapper;

import com.OSS.Health.model.MysqlDataModel;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MysqlDataMapper {

    // 插入一条记录
    @Insert("INSERT INTO vuejs_core (id, time, number, s1) VALUES (#{id}, #{time}, #{number}, #{s1})")
    void insertMysqlData(MysqlDataModel entity);
    
    //读取数据
    @Select("SELECT time, number, s1 FROM vuejs_core WHERE id = #{id}")
    List<Map<String, Object>> getMysqlDataModel(String id);
    
    @Select("SELECT time, number FROM vuejs_core WHERE id = #{id}")
    List<Map<String, Object>> getMysqlDataModelNoS1(String id);
    
    //清空 vuejs_core
    @Delete("DELETE FROM vuejs_core")
    void clearMysqlDataAll();
    
    @Delete("DELETE FROM vuejs_core WHERE id = #{id}")
    void clearMysqlDataById(String id);
    
    //创建一个新的表
    @Update("CREATE TABLE `${newTable}` LIKE sample_table")
    void createTable(@Param("newTable") String newTable);

    //删除表
    @Update("DROP TABLE IF EXISTS `${tableName}`")
    void dropTable(@Param("tableName") String tableName);

    @Insert("INSERT INTO `${tableName}` (id, time, number, s1) VALUES (#{entity.id}, #{entity.time}, #{entity.number}, #{entity.s1})")
    void insertMysqlData_new(@Param("tableName") String tableName, @Param("entity") MysqlDataModel entity);

    @Select("SELECT time, number, s1 FROM `${tableName}` WHERE id = #{id}")
    List<Map<String, Object>> getMysqlDataModel_new(@Param("tableName") String tableName, @Param("id") String id);

    @Select("SELECT time, number FROM `${tableName}` WHERE id = #{id}")
    List<Map<String, Object>> getMysqlDataModelNoS1_new(@Param("tableName") String tableName, @Param("id") String id);

    @Delete("DELETE FROM `${tableName}`")
    void clearMysqlDataAll_new(@Param("tableName") String tableName);

    @Delete("DELETE FROM `${tableName}` WHERE id = #{id}")
    void clearMysqlDataById_new(@Param("tableName") String tableName, @Param("id") String id);

}
