package com.OSS.Health.mapper;

import com.OSS.Health.model.LongTermContributor;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LongTermContributorMapper {

    // 插入一条记录
    @Insert("INSERT INTO longtermcontributors (time, number) VALUES (#{time}, #{number})")
    void insertLongTermContributor(LongTermContributor entity);
    
    //读取数据
    @Select("SELECT time, number FROM longtermcontributors")
    List<LongTermContributor> getLongTermContributor();
    
    //清空longtermcontributors
    @Delete("DELETE FROM longtermcontributors")
    void clearLongTermContributor();
}
