package com.example.Nadeuri.board;

import com.example.Nadeuri.board.dto.BoardDTO;
import com.example.Nadeuri.board.dto.request.BoardCreateRequest;
import com.example.Nadeuri.board.dto.request.BoardPageRequestDTO;
import com.example.Nadeuri.board.dto.request.BoardUpdateRequest;
import com.example.Nadeuri.board.exception.BoardException;
import com.example.Nadeuri.board.exception.BoardTaskException;
import com.example.Nadeuri.member.MemberEntity;
import com.example.Nadeuri.member.MemberRepository;
import com.example.Nadeuri.member.exception.MemberException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class BoardService {
    private static final Logger log = LoggerFactory.getLogger(BoardService.class);
    private final BoardRepository boardRepository;
    private final ImageRepository imageRepository;
    private final MemberRepository memberRepository;

    @Value("${file.local.upload.path}")
    private String uploadPath;

    /**
     * 게시글 등록
     */
    @Transactional
    public void register(final BoardCreateRequest request, final MultipartFile boardImage) {

            String imageUrl;

            if (boardImage == null || boardImage.isEmpty()) {
                imageUrl = uploadPath + "/defaultImage.png";
            } else {
                imageUrl = imageRepository.upload(boardImage);
            }

            MemberEntity memberEntity = retrieveMember(request.getMemberId());
    try {
            BoardEntity board = BoardEntity.create(
                    memberEntity,
                    request.getBoardTitle(),
                    request.getBoardContent(),
                    request.getCategory(),
                    imageUrl
            );
            boardRepository.save(board);
        } catch (Exception e) {
            throw BoardException.NOT_REGISTERED.get();
        }
    }

    //게시글 상세 조회 (1개 조회)
    public BoardDTO read(Long boardId) {
        BoardEntity board = retrieveBoard(boardId);
        if(! (board.getDeletedAt() == null)) throw BoardException.NOT_FOUND.get();
        return new BoardDTO(board);
    }

        //게시글 전체 조회
    public Page<BoardDTO> page(BoardPageRequestDTO boardPageRequestDTO) {
        try {
            Sort sort = Sort.by("id").ascending();
            Pageable pageable = boardPageRequestDTO.getPageable(sort);
            return boardRepository.pageDTO(pageable);
        } catch (Exception e) {
            throw BoardException.NOT_FOUND.get();
        }
    }

        //게시글 전체 조회 (검색) -- 제목,작성자 검색
    public Page<BoardDTO> pageSearch(String keyword, BoardPageRequestDTO boardPageRequestDTO) {
        try {
            Sort sort = Sort.by("id").ascending();
            Pageable pageable = boardPageRequestDTO.getPageable(sort);
            return boardRepository.pageSearch(keyword,pageable);
        } catch (Exception e) {
            throw BoardException.NOT_FOUND.get();
        }
    }

    /**
     * 게시판 수정
     */
    @Transactional
    public BoardEntity update(
            final Long boardId,
            final BoardUpdateRequest request,
            final MultipartFile boardImage
    ) {
        try{
            MemberEntity memberEntity = retrieveMember(request.getMemberId());
            BoardEntity boardEntity = retrieveBoard(boardId);

            String imageUrl = (boardImage != null && !boardImage.isEmpty())
                    ? imageRepository.upload(boardImage)
                    : uploadPath + "/defaultImage.png";

            boardEntity.update(
                    memberEntity,
                    request.getBoardTitle(),
                    request.getBoardContent(),
                    request.getCategory(),
                    imageUrl
            );
            return boardRepository.save(boardEntity);
        } catch (Exception e){
            log.error(e.getMessage());
            throw BoardException.NOT_MODIFIED.get();
        }

    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public BoardEntity delete(
            final Long boardId
    ) {
        try {
            BoardEntity board = retrieveBoard(boardId);
            board.recordDeletion(LocalDateTime.now());
            return boardRepository.save(board);
        }catch (Exception e){
            throw BoardException.NOT_REMOVED.get();
        }
    }


    private MemberEntity retrieveMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberException.NOT_FOUND::get);
    }

    private BoardEntity retrieveBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(BoardException.NOT_FOUND::get);
    }

}
