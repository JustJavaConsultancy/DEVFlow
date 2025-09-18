package com.justjava.devFlow.tasks;

import org.springframework.stereotype.Service;

@Service
public class FormService {
    private final FormRepository formRepository;

    public FormService(FormRepository formRepository) {
        this.formRepository = formRepository;
    }

    public Form getFormByCode(String formCode) {
        return formRepository.findByFormCode(formCode).orElse(null);
    }
}
