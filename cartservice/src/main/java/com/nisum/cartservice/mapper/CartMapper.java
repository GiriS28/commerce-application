package com.nisum.cartservice.mapper;

import com.nisum.cartservice.entity.Cart;
import com.nisum.cartservice.entity.CartItem;
import com.nisum.cartservice.persistence.entity.CartEntity;
import com.nisum.cartservice.persistence.entity.CartItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    CartEntity toNewEntity(Cart cart);

    @Mapping(
            target = "cartId",
            expression = "java(\"CART-\" + cartEntity.getUserId())"
    )
    Cart toDomain(CartEntity cartEntity);

    @Mapping(target = "items", ignore = true)
    void updateEntity(
            Cart cart,
            @MappingTarget CartEntity cartEntity
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cart", ignore = true)
    CartItemEntity toNewEntity(CartItem cartItem);

    CartItem toDomain(CartItemEntity cartItemEntity);
}