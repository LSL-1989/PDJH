package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.OfficeRoomInput;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.repository.DoctorScheduleRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.repository.ScreenRoomMappingRepository;
import com.gocent.pdjh.repository.SignRoomMappingRepository;
import com.gocent.pdjh.service.DepartmentService;
import com.gocent.pdjh.service.OfficeRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficeRoomServiceImpl implements OfficeRoomService {

    private final OfficeRoomRepository repository;
    private final DepartmentService departmentService;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ScreenRoomMappingRepository screenRoomMappingRepository;
    private final SignRoomMappingRepository signRoomMappingRepository;

    @Override
    public List<OfficeRoom> getAllRooms() {
        return repository.findAll();
    }

    @Override
    public List<OfficeRoom> getRoomsByDeptId(String deptId) {
        return repository.findByDeptId(deptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRoom(OfficeRoomInput input) {
        if (input.getDeptId() == null || input.getDeptId().isBlank()) {
            throw new IllegalArgumentException("必须选择所属科室");
        }
        if (!departmentService.canAttachResources(input.getDeptId().trim())) {
            throw new IllegalArgumentException("只能挂载到叶子科室（无子节点的科室），请勿选择诊区/分组");
        }

        OfficeRoom room;
        boolean isNew = input.getId() == null || input.getId().isEmpty();
        if (!isNew) {
            room = repository.findById(input.getId()).orElseThrow(() -> new IllegalArgumentException("诊室不存在"));
            if (room.getDoctorId() != null && !room.getDoctorId().isBlank()) {
                boolean deptChanging = input.getDeptId() != null && !input.getDeptId().equals(room.getDeptId());
                boolean closing = input.getState() != null && input.getState() == 0
                        && (room.getState() == null || room.getState() != 0);
                if (deptChanging || closing) {
                    throw new IllegalArgumentException("当前诊室有医生坐诊，请先让医生离诊后再修改归属科室或关闭诊室");
                }
            }
        } else {
            room = new OfficeRoom();
            room.setId(UUID.randomUUID().toString());
        }
        BeanUtils.copyProperties(input, room, "id");
        if (!isNew) {
            room.setId(input.getId());
        }
        repository.save(room);
        log.info("保存物理诊室成功: {}", room.getName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRoom(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("诊室 id 不能为空");
        }
        OfficeRoom room = repository.findById(id.trim())
                .orElseThrow(() -> new IllegalArgumentException("诊室不存在"));
        if (room.getDoctorId() != null && !room.getDoctorId().isBlank()) {
            throw new IllegalArgumentException("当前诊室有医生坐诊，无法删除");
        }
        List<?> schedules = doctorScheduleRepository.findByOfficeRoomId(id.trim());
        if (schedules != null && !schedules.isEmpty()) {
            throw new IllegalArgumentException("该诊室已有 " + schedules.size() + " 条医生排班，请先调整排班后再删除");
        }
        screenRoomMappingRepository.findByOfficeRoomId(id.trim()).forEach(screenRoomMappingRepository::delete);
        signRoomMappingRepository.findByOfficeRoomId(id.trim()).forEach(signRoomMappingRepository::delete);
        repository.deleteById(id.trim());
        log.info("删除物理诊室成功: {}", id);
        return true;
    }
}
