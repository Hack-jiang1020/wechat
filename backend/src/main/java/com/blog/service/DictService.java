package com.blog.service;

import com.blog.common.BizException;
import com.blog.entity.DataDict;
import com.blog.repository.DataDictRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DictService {

    private final DataDictRepository dataDictRepository;

    public DictService(DataDictRepository dataDictRepository) {
        this.dataDictRepository = dataDictRepository;
    }

    public List<DataDict> listEnabled() {
        return dataDictRepository.findByStatusOrderBySortAsc(1);
    }

    public List<DataDict> list(String type) {
        if (StringUtils.hasText(type)) {
            return dataDictRepository.findByDictTypeOrderBySortAsc(type.trim());
        }
        return dataDictRepository.findAll();
    }

    public DataDict save(DataDict form) {
        if (!StringUtils.hasText(form.getDictType()) || !StringUtils.hasText(form.getDictLabel())
                || !StringUtils.hasText(form.getDictValue())) {
            throw new BizException("字典类型、名称、值均不能为空");
        }
        if (form.getId() == null) {
            form.setSort(form.getSort() == null ? 0 : form.getSort());
            form.setStatus(form.getStatus() == null ? 1 : form.getStatus());
            form.setCreateTime(LocalDateTime.now());
            return dataDictRepository.save(form);
        }
        DataDict exist = dataDictRepository.findById(form.getId())
                .orElseThrow(() -> new BizException(404, "字典不存在"));
        exist.setDictType(form.getDictType());
        exist.setDictLabel(form.getDictLabel());
        exist.setDictValue(form.getDictValue());
        exist.setSort(form.getSort() == null ? 0 : form.getSort());
        exist.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        exist.setRemark(form.getRemark());
        return dataDictRepository.save(exist);
    }

    public void delete(Long id) {
        if (!dataDictRepository.existsById(id)) {
            throw new BizException(404, "字典不存在");
        }
        dataDictRepository.deleteById(id);
    }
}
