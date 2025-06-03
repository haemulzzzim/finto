package com.haemulzzzim.fintobe.meeting_room.controller;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.haemulzzzim.fintobe.config.AppConfig;
import com.haemulzzzim.fintobe.meeting_room.dto.UserDto;
import com.haemulzzzim.fintobe.meeting_room.entity.Employee;
import com.haemulzzzim.fintobe.meeting_room.service.CustomUserDetails;
import com.haemulzzzim.fintobe.meeting_room.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 페이지 컨트롤러
 * 사용자 관련 웹 페이지를 제공하는 컨트롤러입니다.
 * 데이터 처리는 UserApiController를 통해 수행합니다.
 */
@Controller
@RequestMapping("/page/users")
@RequiredArgsConstructor
@Slf4j
public class UserPageController {
    private final String REDIRECT_LOGIN_PATH = "redirect:/page/users/login";

    private final UserService userService;
    private final AppConfig appConfig;

    @GetMapping("/login")
    public String loginPage(Model model) {
        String error = (String) model.asMap().get("error");
        if (error != null && !error.isEmpty()) {
            model.addAttribute("error", error);
            log.info("error param: {}", error);
        }
        return "user/login-form";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if (user != null) {
            model.addAttribute("message", "로그인 되어 있습니다.");
            return "user/home";
        }
        return REDIRECT_LOGIN_PATH;
    }

    /**
     * 사용자 등록/수정 폼 페이지
     */
    @GetMapping("/form")
    public String showUserForm(@RequestParam(value = "id", required = false) Long id,
            Model model,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return REDIRECT_LOGIN_PATH;
        }

        if (id != null) {
            // API 컨트롤러를 통해 데이터를 조회하는 것이 이상적이지만,
            // 기존 서비스를 재사용하여 데이터 연속성 유지
            Optional<Employee> emp = userService.findById(id);
            if (emp.isPresent()) {
                UserDto dto = UserDto.builder()
                        .empno(emp.get().getEmpno())
                        .name(emp.get().getName())
                        .email(emp.get().getEmail())
                        .build();
                model.addAttribute("employeeDto", dto);
                model.addAttribute("empSeq", id);
            }
        } else {
            model.addAttribute("employeeDto", new UserDto());
        }
        return "user/user-form";
    }

    /**
     * 사용자 등록/수정 처리
     * 데이터 처리는 API 컨트롤러를 통해 이루어지는 것이 이상적이지만,
     * 기존 서비스를 활용하여 일관성 유지
     */
    @PostMapping("/save")
    public String saveUser(@RequestParam(value = "empSeq", required = false) Long empSeq,
            @Valid @ModelAttribute("employeeDto") UserDto dto,
            BindingResult result,
            Model model,
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes redirectAttributes) {
        if (user == null) {
            return REDIRECT_LOGIN_PATH;
        }

        if (result.hasErrors()) {
            model.addAttribute("empSeq", empSeq);
            return "user/user-form";
        }

        try {
            if (empSeq == null) {
                userService.save(dto);
                redirectAttributes.addFlashAttribute("message", "사용자가 등록되었습니다.");
            } else {
                userService.update(empSeq, dto);
                redirectAttributes.addFlashAttribute("message", "사용자 정보가 수정되었습니다.");
            }
            return "redirect:" + appConfig.getProxyPath() + "/page/users/list";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("empSeq", empSeq);
            return "user/user-form";
        }
    }

    /**
     * 사용자 목록 페이지
     */
    @GetMapping("/list")
    public String listUsers(Model model, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return REDIRECT_LOGIN_PATH;
        }

        // API 컨트롤러를 통해 데이터를 조회하는 것이 이상적이지만,
        // 기존 서비스 로직 재사용
        model.addAttribute("employees", userService.findAll());
        return "user/user-list";
    }

    /**
     * 사용자 삭제 처리
     * 보안을 위해 GET에서 POST로 변경
     */
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes redirectAttributes) {
        if (user == null) {
            return REDIRECT_LOGIN_PATH;
        }

        try {
            userService.delete(id);
            redirectAttributes.addFlashAttribute("message", "사용자가 삭제되었습니다.");
            return "redirect:" + appConfig.getProxyPath() + "/page/users/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + appConfig.getProxyPath() + "/page/users/list";
        }
    }
}