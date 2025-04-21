package com.grash.utils;

import com.grash.model.File;
import com.grash.model.OwnUser;
import com.grash.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileTestUtils {

    @Autowired
    private FileService fileService;
    @Autowired
    private UserTestUtils userTestUtils;

    public File generateFile(OwnUser clinician) {
        userTestUtils.setCurrentUser(clinician);
        File file = new File();
        file.setName(TestHelper.generateString());
        file.setPath(TestHelper.generateString());
        return fileService.create(file);

    }
}
