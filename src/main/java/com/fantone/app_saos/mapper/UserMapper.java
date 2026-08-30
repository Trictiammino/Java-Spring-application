package com.fantone.app_saos.mapper;

import com.fantone.app_saos.dto.request.AuthRequestDto;
import com.fantone.app_saos.dto.response.UserDto;
import com.fantone.app_saos.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);

//    @Mapping(target = "username", source = "username")
//    @Mapping(target = "password", source = "password") // sarà codificata dopo
//    @Mapping(target = "name", source = "name")
//    @Mapping(target = "lastname", source = "lastname")
//    @Mapping(target = "email", source = "email")
//    @Mapping(target = "address", source = "address")
//    @Mapping(target = "age", source = "age")
//    @Mapping(target = "role", ignore = true) // sarà impostata dal service
//    @Mapping(target = "created_at", ignore = true) // gestita automaticamente
//    @Mapping(target = "refreshTokens", ignore = true)
    User toEntity(AuthRequestDto authRequestDto);
}
