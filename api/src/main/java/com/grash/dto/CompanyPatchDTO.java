package com.grash.dto;

import com.grash.model.File;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class CompanyPatchDTO {
    @NotNull
    private String name;
    private String address;
    private String phone;
    private String website;
    private String email;
    private File logo;
    private String city;
    private String state;
    private String zipCode;
}
