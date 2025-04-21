package com.grash.utils;

import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.service.RoleService;
import com.grash.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class RoleTestUtils {

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;

    public Role generateRoleOnlyForCompany(OwnUser user) {
        Role role = new Role();
        role.setRoleType(RoleType.ROLE_CLIENT);
        role.setName(TestHelper.generateString());
        role.setCompanySettings(user.getCompany().getCompanySettings());
        return roleService.create(role);

    }
}
