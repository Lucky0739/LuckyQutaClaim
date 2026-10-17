package com.lucky.quota.activity.model.vo;

import com.lucky.quota.activity.model.entity.QuotaActivity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaActivityVO {

    private Long id;

    private String activityName;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private Integer status;

    private String description;

    public static QuotaActivityVO fromEntity(QuotaActivity activity) {
        if (activity == null) {
            return null;
        }
        return QuotaActivityVO.builder()
                .id(activity.getId())
                .activityName(activity.getActivityName())
                .beginTime(activity.getBeginTime())
                .endTime(activity.getEndTime())
                .status(activity.getStatus())
                .description(activity.getDescription())
                .build();
    }
}
