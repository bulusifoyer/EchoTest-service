package com.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.testplatform.entity.Environment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 环境配置数据访问接口
 * 继承 MyBatis-Plus 的 BaseMapper，提供基础的 CRUD 操作
 * 同时提供基于 project_id 的关联查询方法
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Mapper
public interface EnvironmentMapper extends BaseMapper<Environment> {

    /**
     * 根据项目ID查询环境配置列表
     * 获取指定项目下的所有环境配置
     *
     * @param projectId 项目ID
     * @return 环境配置列表
     */
    @Select("SELECT * FROM t_environment WHERE project_id = #{projectId} ORDER BY update_time DESC")
    List<Environment> selectListByProjectId(@Param("projectId") Long projectId);

    /**
     * 根据环境名称和项目ID查询环境配置
     * 用于检查同一项目下是否存在同名环境
     *
     * @param envName   环境名称
     * @param projectId 项目ID
     * @return 环境配置实体，如果不存在则返回 null
     */
    @Select("SELECT * FROM t_environment WHERE env_name = #{envName} AND project_id = #{projectId}")
    Environment selectByEnvNameAndProjectId(@Param("envName") String envName, @Param("projectId") Long projectId);
}
