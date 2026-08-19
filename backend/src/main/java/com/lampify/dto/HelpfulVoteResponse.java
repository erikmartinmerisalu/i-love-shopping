package com.lampify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HelpfulVoteResponse {
    private long helpfulCount;
    private boolean helpfulByCurrentUser;
}
