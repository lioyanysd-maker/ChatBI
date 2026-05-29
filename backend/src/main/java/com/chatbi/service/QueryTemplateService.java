package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.dto.request.QueryTemplateRequest;
import com.chatbi.dto.response.QueryTemplateResponse;
import com.chatbi.entity.QueryTemplate;
import com.chatbi.exception.BusinessException;
import com.chatbi.mapper.QueryTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueryTemplateService {

    private final QueryTemplateMapper queryTemplateMapper;
    private final DataSourceService dataSourceService;

    public List<QueryTemplateResponse> listTemplates(Long dataSourceId) {
        LambdaQueryWrapper<QueryTemplate> wrapper = new LambdaQueryWrapper<QueryTemplate>()
                .orderByAsc(QueryTemplate::getSortOrder)
                .orderByDesc(QueryTemplate::getCreatedAt);
        if (dataSourceId != null) {
            wrapper.and(w -> w.isNull(QueryTemplate::getDataSourceId)
                    .or()
                    .eq(QueryTemplate::getDataSourceId, dataSourceId));
        } else {
            wrapper.isNull(QueryTemplate::getDataSourceId);
        }
        return queryTemplateMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public QueryTemplateResponse create(QueryTemplateRequest request) {
        if (request.getDataSourceId() != null) {
            dataSourceService.requireEntity(request.getDataSourceId());
        }
        QueryTemplate entity = new QueryTemplate();
        entity.setDataSourceId(request.getDataSourceId());
        entity.setTitle(request.getTitle().trim());
        entity.setQuestion(request.getQuestion().trim());
        entity.setCategory(request.getCategory() != null ? request.getCategory() : "query");
        entity.setChartType(request.getChartType() != null ? request.getChartType() : "auto");
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setCreatedAt(LocalDateTime.now());
        queryTemplateMapper.insert(entity);
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        QueryTemplate entity = queryTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "常用问题不存在");
        }
        queryTemplateMapper.deleteById(id);
    }

    private QueryTemplateResponse toResponse(QueryTemplate entity) {
        return QueryTemplateResponse.builder()
                .id(entity.getId())
                .dataSourceId(entity.getDataSourceId())
                .title(entity.getTitle())
                .question(entity.getQuestion())
                .category(entity.getCategory())
                .chartType(entity.getChartType())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
