package com.grash.dto;

import com.grash.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserInvitationDTO {
    @NotNull
    private Role role;

    private Collection<String> emails = new ArrayList<>();
}
