package com.campusloop.service.dto;

/**
 * 用户注册/资料表单对象（界面层可直接与 Swing 输入框绑定）。
 */
public class UserCommand {

    private String username;
    private String password;
    private String confirmPassword;
    private String nickname;
    private String contact;

    public String getUsername() {
        return username;
    }

    public UserCommand setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserCommand setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public UserCommand setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
        return this;
    }

    public String getNickname() {
        return nickname;
    }

    public UserCommand setNickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public String getContact() {
        return contact;
    }

    public UserCommand setContact(String contact) {
        this.contact = contact;
        return this;
    }
}
