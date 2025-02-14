package com.OSS.Health.mapper;

import com.OSS.Health.model.MysqlDataModel;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
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
}
