package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.Spu;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 〈一句话功能简述〉<br> 
 * 〈〉
 *
 * @author njtech
 * @create 2019/4/1
 * @since 1.0.0
 */
@Service
public class GoodsService {
    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    //private SpuDetailMapper spuDetailMapper;
    private SpuDetailMapper detailMapper;

    @Autowired
   private CategoryService categoryService;
    @Autowired
    private BrandService brandService;


    /**
     *
     * @param page
     * @param rows
     * @param key
     * @param saleable
     * @return
     */
    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, String key, Boolean saleable) {
        //分页
        PageHelper.startPage(page, rows);
        //过滤
        Example example = new Example(Spu.class);
          //搜索字段过滤
        Example.Criteria criteria = example.createCriteria();

        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        //上下架过滤
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);//原版是orEqualTo
        }
        //默认以上一次更新时间排序
        example.setOrderByClause("last_update_time desc");//原版
        //example.setOrderByClause("lastUpdateTime DESC");

        //只查询未删除的商品
        criteria.andEqualTo("valid", 1);

        //查询
        List<Spu> spus = spuMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(spus)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //对查询结果中的分类名和品牌名进行处理，解析
        //handleCategoryAndBrand(spus);原版本
        loadCategoryAndBrandName(spus);

        //解析分页结果
        PageInfo<Spu> info = new PageInfo<>(spus);

        return new PageResult<>(info.getTotal(), spus);
    }

    private void loadCategoryAndBrandName(List<Spu>spus){
        for (Spu spu : spus) {
            //根据spu中的分类ids查询分类名
            //处理分类名称
            //原版本是names
            List<String> names = categoryService.queryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream()
                    .map(Category::getName)
                    .collect(Collectors.toList());
            //对分类名进行处理
            spu.setCname(StringUtils.join(names, "/"));

            //查询品牌
            //spu.setBname(brandMapper.selectByPrimaryKey(spu.getBrandId()).getName());//老版本
            spu.setBname(brandService.queryById(spu.getBrandId()).getName());
        }
    }
}