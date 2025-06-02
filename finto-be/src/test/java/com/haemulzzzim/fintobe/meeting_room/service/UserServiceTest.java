package com.haemulzzzim.fintobe.meeting_room.service;

import com.haemulzzzim.fintobe.meeting_room.dto.UserDto;
import com.haemulzzzim.fintobe.meeting_room.entity.Employee;
import com.haemulzzzim.fintobe.meeting_room.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_성공() {
        UserDto dto = UserDto.builder().empno("123456").name("홍길동").email("hong@test.com").build();
        Employee emp = Employee.builder().empno("123456").name("홍길동").email("hong@test.com").build();
        when(userRepository.save(any(Employee.class))).thenReturn(emp);

        Employee result = userService.save(dto);
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getEmpno()).isEqualTo("123456");
    }

    @Test
    void update_성공() {
        UserDto dto = UserDto.builder().empno("654321").name("김철수").email("kim@test.com").build();
        Employee emp = Employee.builder().empSeq(1L).empno("123456").name("홍길동").email("hong@test.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(userRepository.save(any(Employee.class))).thenReturn(emp);

        Employee result = userService.update(1L, dto);
        assertThat(result.getName()).isEqualTo("김철수");
        assertThat(result.getEmpno()).isEqualTo("654321");
    }

    @Test
    void update_존재하지않음_예외() {
        UserDto dto = UserDto.builder().empno("654321").name("김철수").email("kim@test.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.update(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 사원");
    }

    @Test
    void delete_성공() {
        doNothing().when(userRepository).deleteById(1L);
        userService.delete(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void findById_성공() {
        Employee emp = Employee.builder().empSeq(1L).empno("123456").name("홍길동").email("hong@test.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(emp));
        Optional<Employee> result = userService.findById(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("홍길동");
    }

    @Test
    void findById_존재하지않음() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        Optional<Employee> result = userService.findById(1L);
        assertThat(result).isNotPresent();
    }

    @Test
    void findAll_성공() {
        Employee emp1 = Employee.builder().empSeq(1L).name("홍길동").build();
        Employee emp2 = Employee.builder().empSeq(2L).name("김철수").build();
        when(userRepository.findAll()).thenReturn(Arrays.asList(emp1, emp2));
        List<Employee> result = userService.findAll();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("홍길동");
        assertThat(result.get(1).getName()).isEqualTo("김철수");
    }
}