package com.OSS.Health.mapper;

import com.OSS.Health.model.MysqlWeightModel;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MysqlWeightMapper {

    // 插入一条记录
    @Insert("INSERT INTO cfg_metric_weight (id, weight) VALUES (#{id}, #{weight})")
    void insertMysqlWeight(MysqlWeightModel entity);
    
    //读取数据
    @Select("SELECT id, weight FROM cfg_metric_weight")
    List<Map<String, Object>> getMysqlWeightModel();
    
    //清空 cfg_metric_weight
    @Delete("DELETE FROM cfg_metric_weight")
    void clearMysqlWeightAll();
    
    @Delete("DELETE FROM cfg_metric_weight WHERE id = #{id}")
    void clearMysqlWeightById(String id);
}
