package com.bootcamp.be_java_hisp_w25_g02.service;

import com.bootcamp.be_java_hisp_w25_g02.dto.request.PostWithDiscountDTO;
import com.bootcamp.be_java_hisp_w25_g02.dto.response.*;

import com.bootcamp.be_java_hisp_w25_g02.dto.request.PostDTO;
import com.bootcamp.be_java_hisp_w25_g02.entity.Post;
import com.bootcamp.be_java_hisp_w25_g02.entity.Product;
import com.bootcamp.be_java_hisp_w25_g02.entity.User;
import com.bootcamp.be_java_hisp_w25_g02.repository.IPostRepository;
import com.bootcamp.be_java_hisp_w25_g02.repository.IUserRepository;
import com.bootcamp.be_java_hisp_w25_g02.repository.PostRepositoryImpl;
import com.bootcamp.be_java_hisp_w25_g02.exception.BadRequestException;
import com.bootcamp.be_java_hisp_w25_g02.exception.NotFoundException;
import com.bootcamp.be_java_hisp_w25_g02.repository.UserRepositoryImpl;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PostServiceImpl implements IPostService{

    IPostRepository postRepository;
    IUserService userService;
    IUserRepository userRepository;
    public PostServiceImpl(PostRepositoryImpl postRepository, UserServiceImpl userService, UserRepositoryImpl userRepository){
        this.postRepository = postRepository;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    public GenericResponseDTO savePost(PostDTO postDTO){
        if (!userService.existUser(postDTO.user_id())){
            throw new BadRequestException("El usuario no existe");
        }
        if (!userService.isSeller(postDTO.user_id())){
            throw new BadRequestException("El usuario no es vendedor");
        }
        if (this.postRepository.findProductById(postDTO.product().product_id()).isPresent()){
            throw new BadRequestException("Ya existe un Producto con ese ID");
        }

        Post post = mapDtoToPost(postDTO);
        long id = this.postRepository.savePost(post);
        return new GenericResponseDTO("Post creado con exito con el id: " + id);
    }
   
    @Override
    public FollowingPostDTO getPostsOrderedByDate(Integer userId, String order) {
        if (!order.equalsIgnoreCase("date_asc") && !order.equalsIgnoreCase("date_desc") ){
            throw new BadRequestException("Parametro order no reconocido. Debe tener el valor date_asc o date_desc");
        }
        List<Integer> followedUsers = userService.getFollowedUsersId(userId);
        List<Post> posts = new ArrayList<>();
        for(Integer sellerId: followedUsers){
            posts.addAll(this.postRepository.findByUserId(sellerId));

        }

        if (posts.isEmpty()){
            throw new NotFoundException("No hay post de los usuarios seguidos en las ultimas 2 semanas");
        }
        if( order.equalsIgnoreCase("date_asc")){
            posts = posts.stream().sorted(Comparator.comparing(Post::getPostDate)).toList();
        } else {
            posts = posts.stream().sorted(Comparator.comparing(Post::getPostDate).reversed()).toList();
        }
        return new FollowingPostDTO(userId, posts.stream().map(this::mapPostToDTO).toList());
    }

    @Override
    public GenericResponseDTO savePostWithDiscount(PostWithDiscountDTO postWithDiscountDTO) {
        if (discountDtoHasErrors(postWithDiscountDTO)) {
            throw new BadRequestException("Datos del post mal formados o incompletos.");
        }
        long id = postRepository.savePost(mapDiscountDtoToPost(postWithDiscountDTO));
        return new GenericResponseDTO("Post con descuento creado con exito con el id: " + id);
    }

    @Override
    public PromoPostsCountDTO getPromoPostsCount(Integer userId) {
        Optional<User> user = getUserById(userId);
        List<Post> postList = getPostsByUserId(userId);

        List<Post> promoPostsList = postList.stream()
                .filter(post -> post.getHas_promo() != null && post.getHas_promo()).toList();
        if (promoPostsList.isEmpty()) {
            throw new NotFoundException("El usuario solicitado no posee posts en promoción.");
        }

        return new PromoPostsCountDTO(userId, user.get().getUser_name(), promoPostsList.size());
    }

    @Override
    public SellerPromoPostsDTO getPromoPosts(Integer userId) {
        Optional<User> user = getUserById(userId);
        List<Post> postList = getPostsByUserId(userId);

        List<Post> promoPostsList = postList.stream()
                .filter(post -> post.getHas_promo() != null && post.getHas_promo()).toList();
        if (promoPostsList.isEmpty()) {
            throw new NotFoundException("El usuario solicitado no posee posts en promoción.");
        }

        return new SellerPromoPostsDTO(
                userId,
                user.get().getUser_name(),
                promoPostsList.stream().map(this::mapPostToRespDto).toList()
        );
    }

    // ###### Private Methods ######
    private PostDTO mapPostToDTO(Post post){
        return new PostDTO(post.getUser_id(), post.getPostDate(), mapToProductDTO(post.getProduct()),post.getCategory(), post.getPrice());

    }

    private ProductDTO mapToProductDTO(Product product){
        return new ProductDTO(product.getProduct_id(), product.getProduct_name(), product.getType(), product.getBrand(), product.getColor(), product.getNotes());
    }

    private Post mapDtoToPost(PostDTO postDTO){
        return new Post(0,
                postDTO.user_id(),
                postDTO.date(),
                new Product(
                        postDTO.product().product_id(),
                        postDTO.product().product_name(),
                        postDTO.product().type(),
                        postDTO.product().brand(),
                        postDTO.product().color(),
                        postDTO.product().notes()
                ),
                postDTO.category(),
                postDTO.price());
    }
    private Post mapDiscountDtoToPost(PostWithDiscountDTO postWithDiscountDTO){
        return new Post(0,
                postWithDiscountDTO.user_id(),
                postWithDiscountDTO.date(),
                new Product(
                        postWithDiscountDTO.product().product_id(),
                        postWithDiscountDTO.product().product_name(),
                        postWithDiscountDTO.product().type(),
                        postWithDiscountDTO.product().brand(),
                        postWithDiscountDTO.product().color(),
                        postWithDiscountDTO.product().notes()
                ),
                postWithDiscountDTO.category(),
                postWithDiscountDTO.price(),
                postWithDiscountDTO.has_promo(),
                postWithDiscountDTO.discount()
        );
    }

    private PostRespDTO mapPostToRespDto(Post post) {
        return new PostRespDTO(
                post.getPost_id(),
                post.getUser_id(),
                post.getPostDate(),
                post.getProduct(),
                post.getCategory(),
                post.getPrice(),
                post.getHas_promo(),
                post.getDiscount()
        );
    }

    private boolean discountDtoHasErrors(PostWithDiscountDTO postWithDiscountDTO) {
        return postWithDiscountDTO.user_id() == null ||
                postWithDiscountDTO.date() == null ||
                postWithDiscountDTO.product() == null ||
                postWithDiscountDTO.category() == null ||
                postWithDiscountDTO.price() == null ||
                postWithDiscountDTO.has_promo() == null ||
                postWithDiscountDTO.discount() == null;
    }

    private Optional<User> getUserById(Integer userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new NotFoundException("El usuario solicitado no existe.");
        }
        return user;
    }

    private List<Post> getPostsByUserId(Integer userId) {
        List<Post> postList = postRepository.findAll().stream()
                .filter(post -> post.getUser_id().equals(userId))
                .toList();
        if (postList.isEmpty()) {
            throw new NotFoundException("El usuario solicitado no posee posts.");
        }
        return postList;
    }
}
