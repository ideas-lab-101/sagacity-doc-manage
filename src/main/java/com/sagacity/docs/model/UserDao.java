package com.sagacity.docs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDao {

    private String user_id;
    private String open_id;
    private String account;
    private String nick_name;
    private String mobile_phone;
    private String avatar_url;
    private int level;
    private Role roleInfo;

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Role{
        private int role_id;
        private String role_name;
        private String role_desc;
    }
}
