package com.bootcamp.be_java_hisp_w25_g02.service;

import com.bootcamp.be_java_hisp_w25_g02.dto.request.PostWithDiscountDTO;
import com.bootcamp.be_java_hisp_w25_g02.dto.response.GenericResponseDTO;
import com.bootcamp.be_java_hisp_w25_g02.dto.request.PostDTO;
import com.bootcamp.be_java_hisp_w25_g02.dto.response.FollowingPostDTO;
import com.bootcamp.be_java_hisp_w25_g02.dto.response.PromoPostsCountDTO;
import com.bootcamp.be_java_hisp_w25_g02.dto.response.SellerPromoPostsDTO;


public interface IPostService {
    GenericResponseDTO savePost(PostDTO post);
    FollowingPostDTO getPostsOrderedByDate(Integer userId, String order);
    GenericResponseDTO savePostWithDiscount(PostWithDiscountDTO postWithDiscountDTO);
    PromoPostsCountDTO getPromoPostsCount(Integer userId);
    SellerPromoPostsDTO getPromoPosts(Integer userId);
}
