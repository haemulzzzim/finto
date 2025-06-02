package com.haemulzzzim.fintobe.meeting_room.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.haemulzzzim.fintobe.exception.ErrorCode;
import com.haemulzzzim.fintobe.exception.business.EntityAlreadyExistException;
import com.haemulzzzim.fintobe.meeting_room.entity.Dept;
import com.haemulzzzim.fintobe.meeting_room.repository.DeptRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeptService {
    private final DeptRepository deptRepository;

    @Transactional
    public Dept createDept(Dept dept) {
        if (deptRepository.existsByName(dept.getName())) {
            throw new EntityAlreadyExistException("이미 존재하는 부서명입니다.", ErrorCode.USER_ALREADY_EXIST);
        }
        return deptRepository.save(dept);
    }

    @Transactional
    public Dept updateDept(Integer id, Dept dept) {
        Dept existing = deptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부서입니다."));
        if (!existing.getName().equals(dept.getName()) && deptRepository.existsByName(dept.getName())) {
            throw new EntityAlreadyExistException("이미 존재하는 부서명입니다.", ErrorCode.USER_ALREADY_EXIST);
        }
        existing.setName(dept.getName());
        existing.setLocation(dept.getLocation());
        return deptRepository.save(existing);
    }
}